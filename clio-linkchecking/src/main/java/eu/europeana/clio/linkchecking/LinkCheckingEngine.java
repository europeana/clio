package eu.europeana.clio.linkchecking;

import com.mongodb.MongoClient;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.LinkType;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides core functionality for the link checking module of Clio.
 */
public final class LinkCheckingEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinkCheckingEngine.class);

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
   * This method starts a new run for the give dataset. It determines which links are to be part of
   * the run, but it doesn't perform any link checking.
   *
   * @param datasetId The (Metis) dataset ID of the dataset for which to create the run.
   * @throws ClioException In case of a problem with accessing or storing the required data.
   */
  public void createRunWithUncheckedLinksForDataset(String datasetId) throws ClioException {

    // Obtain the dataset from the Metis database
    final MongoClientProvider<ConfigurationException> mongoClientProvider = new MongoClientProvider<>(
            properties.getMongoProperties());
    final Dataset dataset;
    try (final MongoClient mongoClient = mongoClientProvider.createMongoClient()) {
      dataset = new MongoCoreDao(mongoClient, properties.getMongoDatabase())
              .getPublishedDatasetById(datasetId);
    }

    // Obtain the sample records from the Solr database.
    final SolrClientProvider<ConfigurationException> solrClientProvider = new SolrClientProvider<>(
            properties.getSolrProperties());
    final List<SampleRecord> sampleRecords;
    try (final CompoundSolrClient solrClient = solrClientProvider.createSolrClient()) {
      sampleRecords = new SolrDao(solrClient.getSolrClient()).getRandomSampleRecords(datasetId,
              properties.getLinkCheckingSampleRecordsPerDataset());
    } catch (IOException e) {
      throw new PersistenceException("Problem occurred while obtaining a sample record.", e);
    }

    // Save the information to the Clio database
    final ClioPersistenceConnection databaseConnection = properties.createPersistenceConnection();
    new DatasetDao(databaseConnection).createOrUpdateDataset(dataset);
    final long runId = new RunDao(databaseConnection).createRunStartingNow(dataset.getDatasetId());
    final LinkDao linkDao = new LinkDao(databaseConnection);
    for (SampleRecord record : sampleRecords) {
      for (Entry<LinkType, Set<String>> links : record.getLinks().entrySet()) {
        for (String url : links.getValue()) {
          linkDao.createUncheckedLink(runId, record.getRecordId(), url, links.getKey());
        }
      }
    }
  }

  /**
   * This method performs link checking on all unchecked links currently in persistence.
   *
   * @throws ClioException In case of a problem with accessing or storing the required data.
   */
  public void performLinkCheckingOnAllUncheckedLinks() throws ClioException {
    final LinkDao linkDao = new LinkDao(properties.createPersistenceConnection());
    try (final LinkChecker linkChecker = createLinkChecker()) {
      boolean linksChecked;
      do {
        linksChecked = checkNextLinkIfAvailable(linkChecker, linkDao);
      } while (linksChecked);
    } catch (IOException e) {
      throw new ClioException("Could not close link checker.", e);
    }
  }

  private static boolean checkNextLinkIfAvailable(final LinkChecker linkChecker, LinkDao linkDao)
          throws PersistenceException {

    // Get the next link
    final Link linkToCheck = linkDao.getAnyUncheckedLink();
    if (linkToCheck == null) {
      return false;
    }

    // Check the link
    LOGGER.info("Checking link {}.", linkToCheck.getLinkUrl());
    String error = null;
    try {
      linkChecker.performLinkChecking(linkToCheck.getLinkUrl());
    } catch (LinkCheckingException e) {
      error = convertToErrorString(e);
    }

    // Save the result
    linkDao.registerLinkChecking(linkToCheck.getLinkUrl(), error);
    return true;
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
