package eu.europeana.clio.linkchecking;

import com.mongodb.MongoClient;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.LinkType;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.dao.BatchDao;
import eu.europeana.clio.common.persistence.dao.DatasetDao;
import eu.europeana.clio.common.persistence.dao.LinkDao;
import eu.europeana.clio.common.persistence.dao.RunDao;
import eu.europeana.clio.linkchecking.config.PropertiesHolder;
import eu.europeana.clio.linkchecking.dao.MongoCoreDao;
import eu.europeana.clio.linkchecking.dao.SolrDao;
import eu.europeana.clio.linkchecking.model.SampleRecord;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.exception.LinkCheckingException;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mongo.MongoClientProvider;
import eu.europeana.metis.solr.CompoundSolrClient;
import eu.europeana.metis.solr.SolrClientProvider;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides core functionality for the link checking module of Clio.
 */
public final class LinkCheckingEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinkCheckingEngine.class);

  private static final int NUMBER_OF_CONCURRENT_THREADS_PER_SERVER = 1;

  private static final Duration UNUSED_SEMAPHORE_GRACE_TIME = Duration.ofSeconds(1);

  private static final Map<String, Semaphore> semaphorePerServer = new ConcurrentHashMap<>();

  private final PropertiesHolder properties;

  /**
   * Constrcutor.
   *
   * @param properties The properties of this module.
   */
  public LinkCheckingEngine(PropertiesHolder properties) {
    this.properties = properties;
  }

  /**
   * This method starts a new run for all datasets that have published data. It determines which
   * links are to be part of the run, but it doesn't perform any link checking. This method is not
   * thread safe: it assumes that no other process is adding runs at the moment.
   *
   * @throws ClioException In case of a problem with accessing or storing the required data.
   */
  public void createRunsForAllAvailableDatasets() throws ClioException {

    // Create access for the mongo (metis core) and the solr.
    final MongoClientProvider<ConfigurationException> mongoClientProvider = new MongoClientProvider<>(
            properties.getMongoProperties());
    final SolrClientProvider<ConfigurationException> solrClientProvider = new SolrClientProvider<>(
            properties.getSolrProperties());

    // Create closable connections to the mongo, the solr and the own database.
    try (
        final ClioPersistenceConnection databaseConnection =
            properties.getPersistenceConnectionProvider().createPersistenceConnection();
        final CompoundSolrClient solrClient = solrClientProvider.createSolrClient();
        final MongoClient mongoClient = mongoClientProvider.createMongoClient()) {

      // Finish setting up the connections.
      final MongoCoreDao mongoCoreDao = new MongoCoreDao(mongoClient,
              properties.getMongoDatabase());
      // See https://github.com/spotbugs/spotbugs/issues/756
      @SuppressWarnings("findbugs:RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
      final SolrClient nativeSolrClient = solrClient.getSolrClient();
      final SolrDao solrDao = new SolrDao(nativeSolrClient);

      // Set up a batch.
      final BatchDao batchDao = new BatchDao(databaseConnection);
      final long batchId = batchDao.createBatchStartingNow(solrDao.getLastUpdateTime(),
              mongoCoreDao.getLastUpdateTime());

      // Trigger checking all datasets and wait for the result.
      final AtomicInteger datasetsAlreadyRunningCounter = new AtomicInteger();
      final AtomicInteger datasetsNotYetIndexedCounter = new AtomicInteger();
      final Stream<String> datasetIds = mongoCoreDao.getAllDatasetIds();
      ParallelTaskExecutor.executeAndWait(datasetIds,
              id -> createRunWithUncheckedLinksForDataset(mongoCoreDao, solrDao, databaseConnection,
                      id, batchId, datasetsAlreadyRunningCounter, datasetsNotYetIndexedCounter),
              properties.getLinkCheckingRunCreateThreads());

      // Set the counters.
      batchDao.setCountersForBatch(batchId, datasetsAlreadyRunningCounter.get(),
              datasetsNotYetIndexedCounter.get());

    } catch (IOException e) {
      throw new PersistenceException("Problem occurred while connecting to data sources.", e);
    }
  }

  private void createRunWithUncheckedLinksForDataset(MongoCoreDao mongoCoreDao, SolrDao solrDao,
          ClioPersistenceConnection databaseConnection, String datasetId, long batchId,
          AtomicInteger datasetsAlreadyRunningCounter,
          AtomicInteger datasetsNotYetIndexedCounter) throws ClioException {

    // If the dataset already has a run in progress, we don't proceed.
    final RunDao runDao = new RunDao(databaseConnection);
    if (runDao.datasetHasActiveRun(datasetId)) {
      LOGGER.info("Skipping dataset {} as it already has an active run.", datasetId);
      datasetsAlreadyRunningCounter.incrementAndGet();
      return;
    }

    // Check and get the dataset from the Metis database
    final Dataset dataset = mongoCoreDao.getPublishedDatasetById(datasetId);
    if (dataset == null) {
      LOGGER.info("Skipping dataset {} as it is not currently published.", datasetId);
      datasetsNotYetIndexedCounter.incrementAndGet();
      return;
    }

    // Obtain the sample records from the Solr database.
    final List<SampleRecord> sampleRecords = solrDao
            .getRandomSampleRecords(datasetId, properties.getLinkCheckingSampleRecordsPerDataset());

    // Save the information to the Clio database
    new DatasetDao(databaseConnection).createOrUpdateDataset(dataset);
    final long runId = runDao.createRunStartingNow(dataset.getDatasetId(), batchId);
    final LinkDao linkDao = new LinkDao(databaseConnection);
    for (SampleRecord record : sampleRecords) {
      for (Entry<LinkType, Set<String>> links : record.getLinks().entrySet()) {
        for (String url : links.getValue()) {
          linkDao.createUncheckedLink(runId, record.getRecordId(), record.getRecordLastIndexTime(),
                  url, links.getKey());
        }
      }
    }
    LOGGER.info("Run created for dataset {}.", datasetId);
  }

  /**
   * This method performs link checking on all unchecked links currently in persistence.
   *
   * @throws ClioException In case of a problem with accessing or storing the required data.
   */
  public void performLinkCheckingOnAllUncheckedLinks() throws ClioException {
    final ScheduledExecutorService semaphoreReleasePool = Executors.newScheduledThreadPool(0);
    try (final ClioPersistenceConnection databaseConnection =
            properties.getPersistenceConnectionProvider().createPersistenceConnection();
            final LinkChecker linkChecker = createLinkChecker()) {
      final LinkDao linkDao = new LinkDao(databaseConnection);
      try (final StreamResult<Link> linksToCheck = linkDao.getAllUncheckedLinks()) {
        // See https://github.com/spotbugs/spotbugs/issues/756
        @SuppressWarnings("findbugs:RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
        final Stream<Link> linkStream = linksToCheck.get();
        ParallelTaskExecutor.executeAndWait(linkStream,
                link -> performLinkCheckingOnUncheckedLink(linkChecker, linkDao,
                        semaphoreReleasePool, link),
                properties.getLinkCheckingRunExecuteThreads());
      }
    } catch (IOException e) {
      throw new ClioException("Could not close link checker.", e);
    } finally {
      semaphoreReleasePool.shutdown();
    }
  }

  private Semaphore acquireSemaphoreAndWait(String server) throws InterruptedException {

    // If there is no server to lock, we are done.
    if (server == null) {
      return null;
    }

    // Try to acquire a semaphore that is still current by the time we have acquired it. Note that
    // for threading reasons this iteration is constructed so that only one call to the cache is
    // made per iteration (to avoid status change in between calls).
    Semaphore acquiredSemaphore = null;
    while(true) {

      // Get the current semaphore. We immediately acquire the semaphore so that there is no chance
      // for another thread to come in between.
      final boolean[] freshSemaphoreAcquired = {false};
      final Semaphore currentSemaphore = semaphorePerServer.computeIfAbsent(server, key -> {
        final Semaphore newSemaphore = new Semaphore(NUMBER_OF_CONCURRENT_THREADS_PER_SERVER);
        try {
          newSemaphore.acquire();
          // Set this boolean here, so that if the thread is interrupted, it is still false.
          freshSemaphoreAcquired[0] = true;
        } catch (InterruptedException e) {
          // Mark thread as interrupted, deal with it outside the lambda. Semaphore is not acquired.
          Thread.currentThread().interrupt();
        }
        return newSemaphore;
      });

      // If the thread is interrupted while creating the semaphore, we are done.
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }

      // If we have acquired a semaphore earlier which was deleted, release it.
      if (acquiredSemaphore != null && !currentSemaphore.equals(acquiredSemaphore)) {
        acquiredSemaphore.release();
      }

      // If we have acquired a semaphore earlier that is still current, or if we created a fresh
      // (already acquired) semaphore, we are done. The current semaphore is the acquired one.
      if (currentSemaphore.equals(acquiredSemaphore) || freshSemaphoreAcquired[0]) {
        return currentSemaphore;
      }

      // Acquire the semaphore (can take some time).
      acquiredSemaphore = currentSemaphore;
      acquiredSemaphore.acquire();
    }
  }

  private void scheduleSemaphoreRelease(String server, Semaphore semaphore,
          ScheduledExecutorService semaphoreReleasePool) {

    // If the semaphore is null, do nothing.
    if (semaphore == null) {
      return;
    }

    // Schedule release of the semaphore and ping the cache to signal the change.
    semaphoreReleasePool.schedule(() -> {

      // Release the semaphore.
      semaphore.release();

      // Give a bit of time for other threads to pick up this semaphore.
      try {
        Thread.sleep(0, UNUSED_SEMAPHORE_GRACE_TIME.getNano());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }

      // Try to determine whether the semaphore can be removed, and remove it if possible. This is
      // a heuristic method without guarantees, hence the checks when acquiring the semaphore.
      semaphorePerServer.computeIfPresent(server, (key, value) -> (value.hasQueuedThreads() ||
              value.availablePermits() < NUMBER_OF_CONCURRENT_THREADS_PER_SERVER) ? value : null);

    }, properties.getLinkCheckingMinTimeBetweenSameServerChecks().toMillis(), TimeUnit.MILLISECONDS);
  }

  private void performLinkCheckingOnUncheckedLink(final LinkChecker linkChecker, LinkDao linkDao,
          ScheduledExecutorService semaphoreReleasePool, Link linkToCheck) throws ClioException {

    // Acquire the semaphore, if necessary, and wait until we do.
    final Semaphore semaphore;
    try {
      semaphore = acquireSemaphoreAndWait(linkToCheck.getServer());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    // Check the link and trigger the waiting period before releasing the semaphore.
    final String linkString = linkToCheck.getLinkUrl();
    LOGGER.info("Checking link {}.", linkString);
    String error = null;
    try {
      linkChecker.performLinkChecking(linkString);
    } catch (LinkCheckingException e) {
      LOGGER.debug("Link checking failed for link '{}'.", linkString, e);
      error = convertToErrorString(e);
    } finally {
      scheduleSemaphoreRelease(linkToCheck.getServer(), semaphore, semaphoreReleasePool);
    }

    // Save the result
    linkDao.registerLinkChecking(linkString, error);
  }

  private static String convertToErrorString(Exception exception) {
    final StringBuilder stringBuilder = new StringBuilder();
    Throwable nestedException = exception;
    while (nestedException != null) {
      if (stringBuilder.length() > 0) {
        stringBuilder.append("\n  caused by:\n");
      }
      stringBuilder.append(nestedException.getMessage());
      nestedException = nestedException.getCause();
    }
    return stringBuilder.toString();
  }

  private LinkChecker createLinkChecker() throws ConfigurationException {
    try {
      return properties.createMediaProcessorFactory().createLinkChecker();
    } catch (MediaProcessorException e) {
      throw new ConfigurationException("Could not create link checker.", e);
    }
  }
}
