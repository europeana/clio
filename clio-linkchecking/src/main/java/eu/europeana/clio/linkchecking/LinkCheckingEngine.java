package eu.europeana.clio.linkchecking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.MongoClient;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.LinkType;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.StreamResult;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides core functionality for the link checking module of Clio.
 */
public final class LinkCheckingEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinkCheckingEngine.class);

  private static Cache<String, Instant> lastCheckPerServerCache = null;

  private final PropertiesHolder properties;

  /**
   * Constrcutor.
   *
   * @param properties The properties of this module.
   */
  public LinkCheckingEngine(PropertiesHolder properties) {
    this.properties = properties;
    synchronized (LinkCheckingEngine.class) {
      if (lastCheckPerServerCache == null) {
        lastCheckPerServerCache = Caffeine.newBuilder()
                .expireAfterWrite(properties.getLinkCheckingMinTimeBetweenSameServerChecks())
                .build();
      }
    }
  }

  /**
   * This method starts a new run for all datasets that have published data. It determines which
   * links are to be part of the run, but it doesn't perform any link checking.
   *
   * @throws ClioException In case of a problem with accessing or storing the required data.
   */
  public void createRunsForAllAvailableDatasets() throws ClioException {
    final MongoClientProvider<ConfigurationException> mongoClientProvider = new MongoClientProvider<>(
            properties.getMongoProperties());
    final SolrClientProvider<ConfigurationException> solrClientProvider = new SolrClientProvider<>(
            properties.getSolrProperties());
    try (
        final ClioPersistenceConnection databaseConnection =
            properties.getPersistenceConnectionProvider().createPersistenceConnection();
        final CompoundSolrClient solrClient = solrClientProvider.createSolrClient();
        final MongoClient mongoClient = mongoClientProvider.createMongoClient()) {
      final MongoCoreDao mongoCoreDao = new MongoCoreDao(mongoClient,
              properties.getMongoDatabase());
      final SolrDao solrDao = new SolrDao(solrClient.getSolrClient());
      final Stream<String> datasetIds = mongoCoreDao.getAllDatasetIds();
      ParallelTaskExecutor.executeAndWait(datasetIds,
              id -> createRunWithUncheckedLinksForDataset(mongoCoreDao, solrDao, databaseConnection,
                      id),
              properties.getLinkCheckingRunCreateThreads());
    } catch (IOException e) {
      throw new PersistenceException("Problem occurred while connecting to data sources.", e);
    }
  }

  private void createRunWithUncheckedLinksForDataset(MongoCoreDao mongoCoreDao, SolrDao solrDao,
          ClioPersistenceConnection databaseConnection, String datasetId) throws ClioException {

    // If the dataset already has a run in progress, we don't proceed.
    final RunDao runDao = new RunDao(databaseConnection);
    if (runDao.datasetHasActiveRun(datasetId)) {
      LOGGER.info("Skipping dataset {} as it already has an active run.", datasetId);
      return;
    }

    // Check and get the dataset from the Metis database
    final Dataset dataset = mongoCoreDao.getPublishedDatasetById(datasetId);
    if (dataset == null) {
      LOGGER.info("Skipping dataset {} as it is not currently published.", datasetId);
      return;
    }

    // Obtain the sample records from the Solr database.
    final List<SampleRecord> sampleRecords = solrDao
            .getRandomSampleRecords(datasetId, properties.getLinkCheckingSampleRecordsPerDataset());

    // Save the information to the Clio database
    new DatasetDao(databaseConnection).createOrUpdateDataset(dataset);
    final long runId = runDao.createRunStartingNow(dataset.getDatasetId());
    final LinkDao linkDao = new LinkDao(databaseConnection);
    for (SampleRecord record : sampleRecords) {
      for (Entry<LinkType, Set<String>> links : record.getLinks().entrySet()) {
        for (String url : links.getValue()) {
          linkDao.createUncheckedLink(runId, record.getRecordId(), url, links.getKey());
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
    try (
        final ClioPersistenceConnection databaseConnection =
            properties.getPersistenceConnectionProvider().createPersistenceConnection();
        final LinkChecker linkChecker = createLinkChecker()) {
      final LinkDao linkDao = new LinkDao(databaseConnection);
      try (final StreamResult<Link> linksToCheck = linkDao.getAllUncheckedLinks()) {
        ParallelTaskExecutor.executeAndWait(linksToCheck.get(),
                link -> performLinkCheckingOnUncheckedLink(linkChecker, linkDao, link),
                properties.getLinkCheckingRunExecuteThreads());
      }
    } catch (IOException e) {
      throw new ClioException("Could not close link checker.", e);
    }
  }

  private void performLinkCheckingOnUncheckedLink(final LinkChecker linkChecker, LinkDao linkDao,
          Link linkToCheck) throws ClioException {

    // Wait, if necessary, before we can access the server again.
    if (linkToCheck.getServer() != null) {
      final Instant lastCheckForServer = lastCheckPerServerCache
              .getIfPresent(linkToCheck.getServer());
      if (lastCheckForServer != null) {
        waitUntil(lastCheckForServer
                .plus(properties.getLinkCheckingMinTimeBetweenSameServerChecks()));
      }
    }

    // Check the link and register that we checked the server.
    LOGGER.info("Checking link {}.", linkToCheck.getLinkUrl());
    String error = null;
    try {
      linkChecker.performLinkChecking(linkToCheck.getLinkUrl());
    } catch (LinkCheckingException e) {
      error = convertToErrorString(e);
    } finally {
      if (linkToCheck.getServer() != null) {
        lastCheckPerServerCache.put(linkToCheck.getServer(), Instant.now());
      }
    }

    // Save the result
    linkDao.registerLinkChecking(linkToCheck.getLinkUrl(), error);
  }

  private static void waitUntil(Instant releaseTime) throws ClioException {
    final long waitingTime = ChronoUnit.MILLIS.between(Instant.now(), releaseTime);
    if (waitingTime > 0) {
      try {
        LOGGER.info("Sleeping for {} milliseconds.", waitingTime);
        Thread.sleep(waitingTime);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ClioException("Thread was interrupted", e);
      }
    }
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
