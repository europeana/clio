package eu.europeana.clio.linkchecking.config;

import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mongo.MongoProperties.ReadPreferenceValue;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mongo.MongoProperties;
import eu.europeana.metis.solr.SolrProperties;

/**
 * Contains the properties required for the execution of the link checking functionality.
 */
public class PropertiesHolder {

  private static final String CONFIGURATION_FILE = "application.properties";

  // Mongo metis-core
  private final String[] mongoCoreHosts;
  private final int[] mongoCorePorts;
  private final String mongoCoreAuthenticationDatabase;
  private final String mongoCoreUsername;
  private final String mongoCorePassword;
  private final boolean mongoCoreEnableSsl;
  private final String mongoCoreDatabase;

  // truststore
  private final String truststorePath;
  private final String truststorePassword;

  // Solr/Zookeeper publish
  private final String[] publishSolrHosts;
  private final String[] publishZookeeperHosts;
  private final int[] publishZookeeperPorts;
  private final String publishZookeeperChroot;
  private final String publishZookeeperDefaultCollection;

  // PostGreSQL
  private final String postgresServer;
  private final String postgresUsername;
  private final String postgresPassword;

  // Link checking
  private final int linkCheckingSampleRecordsPerDataset;
  private final int linkCheckingRunCreateThreads;
  private final int linkCheckingRunExecuteThreads;
  private final int linkCheckingMinTimeBetweenSameServerChecks;
  private final int linkCheckingConnectTimeout;
  private final int linkCheckingResponseTimeout;
  private final int linkCheckingDownloadTimeout;

  /**
   * Constructor. Reads the property file and loads the properties.
   */
  public PropertiesHolder() {

    // Load properties file.
    final Properties properties = new Properties();
    try (final InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIGURATION_FILE)) {
      properties.load(stream);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }

    // Mongo metis-core
    mongoCoreHosts = properties.getProperty("mongo.core.hosts").split(",");
    mongoCorePorts = Arrays.stream(properties.getProperty("mongo.core.port").split(","))
        .mapToInt(Integer::parseInt).toArray();
    mongoCoreAuthenticationDatabase = properties.getProperty("mongo.core.authentication.db");
    mongoCoreUsername = properties.getProperty("mongo.core.username");
    mongoCorePassword = properties.getProperty("mongo.core.password");
    mongoCoreEnableSsl = Boolean.parseBoolean(properties.getProperty("mongo.core.enableSSL"));
    mongoCoreDatabase = properties.getProperty("mongo.core.db");

    // truststore
    truststorePath = properties.getProperty("truststore.path");
    truststorePassword = properties.getProperty("truststore.password");

    // Solr/Zookeeper publish
    publishSolrHosts = properties.getProperty("solr.publish.hosts").split(",");
    publishZookeeperHosts = properties.getProperty("zookeeper.publish.hosts").split(",");
    publishZookeeperPorts =
        Arrays.stream(properties.getProperty("zookeeper.publish.port").split(","))
            .mapToInt(Integer::parseInt).toArray();
    publishZookeeperChroot = properties.getProperty("zookeeper.publish.chroot");
    publishZookeeperDefaultCollection =
        properties.getProperty("zookeeper.publish.defaultCollection");

    // PostGreSQL
    postgresServer = properties.getProperty("postgresql.server");
    postgresUsername = properties.getProperty("postgresql.username");
    postgresPassword = properties.getProperty("postgresql.password");

    // Link Checking
    linkCheckingSampleRecordsPerDataset =
        Integer.parseInt(properties.getProperty("linkchecking.sample.records.per.dataset"));
    linkCheckingRunCreateThreads =
        Integer.parseInt(properties.getProperty("linkchecking.run.create.threads"));
    linkCheckingRunExecuteThreads =
        Integer.parseInt(properties.getProperty("linkchecking.run.execute.threads"));
    linkCheckingMinTimeBetweenSameServerChecks = Integer
        .parseInt(properties.getProperty("linkchecking.min.time.between.same.server.checks"));
    linkCheckingConnectTimeout =
        Integer.parseInt(properties.getProperty("linkchecking.connect.timeout"));
    linkCheckingResponseTimeout =
        Integer.parseInt(properties.getProperty("linkchecking.response.timeout"));
    linkCheckingDownloadTimeout =
        Integer.parseInt(properties.getProperty("linkchecking.download.timeout"));
  }

  public String getTruststorePath() {
    return truststorePath;
  }

  public String getTruststorePassword() {
    return truststorePassword;
  }

  public MongoProperties<ConfigurationException> getMongoProperties()
      throws ConfigurationException {
    final MongoProperties<ConfigurationException> properties =
        new MongoProperties<>(ConfigurationException::new);
    properties.setAllProperties(mongoCoreHosts, mongoCorePorts, mongoCoreAuthenticationDatabase,
        mongoCoreUsername, mongoCorePassword, mongoCoreEnableSsl, ReadPreferenceValue.getDefault());
    return properties;
  }

  public SolrProperties<ConfigurationException> getSolrProperties() throws ConfigurationException {
    final SolrProperties<ConfigurationException> properties =
        new SolrProperties<>(ConfigurationException::new);
    properties.setZookeeperHosts(publishZookeeperHosts, publishZookeeperPorts);
    if (StringUtils.isNotBlank(publishZookeeperChroot)) {
      properties.setZookeeperChroot(publishZookeeperChroot);
    }
    if (StringUtils.isNotBlank(publishZookeeperDefaultCollection)) {
      properties.setZookeeperDefaultCollection(publishZookeeperDefaultCollection);
    }
    for (String host : publishSolrHosts) {
      try {
        properties.addSolrHost(new URI(host));
      } catch (URISyntaxException e) {
        throw new ConfigurationException(e.getMessage(), e);
      }
    }
    return properties;
  }

  public String getMongoDatabase() {
    return mongoCoreDatabase;
  }

  /**
   * Create a persistence connection provider.
   *
   * @return The connection.
   */
  public ClioPersistenceConnectionProvider getPersistenceConnectionProvider() {
    return new ClioPersistenceConnectionProvider(postgresServer, postgresUsername,
        postgresPassword);
  }

  public int getLinkCheckingSampleRecordsPerDataset() {
    return linkCheckingSampleRecordsPerDataset;
  }

  public Duration getLinkCheckingMinTimeBetweenSameServerChecks() {
    return Duration.ofMillis(this.linkCheckingMinTimeBetweenSameServerChecks);
  }

  public int getLinkCheckingRunCreateThreads() {
    return linkCheckingRunCreateThreads;
  }

  public int getLinkCheckingRunExecuteThreads() {
    return linkCheckingRunExecuteThreads;
  }

  /**
   * Create a link checker object.
   *
   * @return A link checker object.
   */
  public LinkChecker createLinkChecker() throws ConfigurationException {
    final MediaProcessorFactory mediaProcessorFactory = new MediaProcessorFactory();
    mediaProcessorFactory.setResourceConnectTimeout(this.linkCheckingConnectTimeout);
    mediaProcessorFactory.setResourceResponseTimeout(this.linkCheckingResponseTimeout);
    mediaProcessorFactory.setResourceDownloadTimeout(this.linkCheckingDownloadTimeout);
    try {
      return mediaProcessorFactory.createLinkChecker();
    } catch (MediaProcessorException e) {
      throw new ConfigurationException("Could not create link checker.", e);
    }
  }
}
