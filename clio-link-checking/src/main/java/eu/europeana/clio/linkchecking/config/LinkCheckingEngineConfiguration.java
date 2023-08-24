package eu.europeana.clio.linkchecking.config;

import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;
import eu.europeana.clio.linkchecking.config.properties.LinkCheckingConfigurationProperties;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mongo.connection.MongoProperties;
import eu.europeana.metis.solr.connection.SolrProperties;
import metis.common.config.properties.mongo.MetisCoreMongoConfigurationProperties;
import metis.common.config.properties.postgres.PostgresConfigurationProperties;
import metis.common.config.properties.solr.PublishSolrZookeeperConfigurationProperties;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

/**
 * Class containing configuration for the {@link eu.europeana.clio.linkchecking.execution.LinkCheckingEngine}
 */
public class LinkCheckingEngineConfiguration implements Closeable {

    private final LinkCheckingConfigurationProperties linkCheckingConfigurationProperties;
    private final MetisCoreMongoConfigurationProperties metisCoreMongoConfigurationProperties;
    private final PublishSolrZookeeperConfigurationProperties publishSolrZookeeperConfigurationProperties;
    private final PostgresConfigurationProperties postgresConfigurationProperties;

    private ClioPersistenceConnection clioPersistenceConnection;

    /**
     * Constructor.
     *
     * @param linkCheckingConfigurationProperties the link checking configuration properties
     * @param metisCoreMongoConfigurationProperties the metis core mongo configuration properties
     * @param publishSolrZookeeperConfigurationProperties the publish solr zookeeper configuration properties
     * @param postgresConfigurationProperties the postgres configuration properties
     */
    public LinkCheckingEngineConfiguration(LinkCheckingConfigurationProperties linkCheckingConfigurationProperties,
                                           MetisCoreMongoConfigurationProperties metisCoreMongoConfigurationProperties,
                                           PublishSolrZookeeperConfigurationProperties publishSolrZookeeperConfigurationProperties,
                                           PostgresConfigurationProperties postgresConfigurationProperties) {
        this.linkCheckingConfigurationProperties = linkCheckingConfigurationProperties;

        this.metisCoreMongoConfigurationProperties = metisCoreMongoConfigurationProperties;
        this.publishSolrZookeeperConfigurationProperties = publishSolrZookeeperConfigurationProperties;
        this.postgresConfigurationProperties = postgresConfigurationProperties;
    }

    public ClioPersistenceConnection getClioPersistenceConnection() {
        if (clioPersistenceConnection == null) {
            clioPersistenceConnection = new ClioPersistenceConnectionProvider(
                    postgresConfigurationProperties.getServer(), postgresConfigurationProperties.getUsername(), postgresConfigurationProperties.getPassword())
                    .createPersistenceConnection();
        }
        return clioPersistenceConnection;
    }

    public MongoProperties<ConfigurationException> getMetisCoreMongoProperties()
            throws ConfigurationException {
        final MongoProperties<ConfigurationException> properties =
                new MongoProperties<>(ConfigurationException::new);
        properties.setAllProperties(
                metisCoreMongoConfigurationProperties.getHosts(), metisCoreMongoConfigurationProperties.getPorts(),
                metisCoreMongoConfigurationProperties.getAuthenticationDatabase(), metisCoreMongoConfigurationProperties.getUsername(),
                metisCoreMongoConfigurationProperties.getPassword(), metisCoreMongoConfigurationProperties.isEnableSsl(),
                null, metisCoreMongoConfigurationProperties.getApplicationName());
        return properties;
    }

    public SolrProperties<ConfigurationException> getSolrProperties() throws ConfigurationException {
        final SolrProperties<ConfigurationException> properties =
                new SolrProperties<>(ConfigurationException::new);
        properties.setZookeeperHosts(
                publishSolrZookeeperConfigurationProperties.getHosts(), publishSolrZookeeperConfigurationProperties.getZookeeper().getPorts());
        if (StringUtils.isNotBlank(publishSolrZookeeperConfigurationProperties.getZookeeper().getChroot())) {
            properties.setZookeeperChroot(publishSolrZookeeperConfigurationProperties.getZookeeper().getChroot());
        }
        if (StringUtils.isNotBlank(publishSolrZookeeperConfigurationProperties.getZookeeper().getDefaultCollection())) {
            properties.setZookeeperDefaultCollection(publishSolrZookeeperConfigurationProperties.getZookeeper().getDefaultCollection());
        }
        for (String host : publishSolrZookeeperConfigurationProperties.getHosts()) {
            try {
                properties.addSolrHost(new URI(host));
            } catch (URISyntaxException e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
        }
        return properties;
    }

    /**
     * Create a link checker object.
     *
     * @return A link checker object.
     * @throws ConfigurationException In case the link checker could not be created.
     */
    public LinkChecker createLinkChecker() throws ConfigurationException {
        final MediaProcessorFactory mediaProcessorFactory = new MediaProcessorFactory();
        mediaProcessorFactory.setResourceConnectTimeout(linkCheckingConfigurationProperties.getConnectTimeout());
        mediaProcessorFactory.setResourceResponseTimeout(linkCheckingConfigurationProperties.getResponseTimeout());
        mediaProcessorFactory.setResourceDownloadTimeout(linkCheckingConfigurationProperties.getDownloadTimeout());
        try {
            return mediaProcessorFactory.createLinkChecker();
        } catch (MediaProcessorException e) {
            throw new ConfigurationException("Could not create link checker.", e);
        }
    }

    public MetisCoreMongoConfigurationProperties getMetisCoreMongoConfigurationProperties() {
        return metisCoreMongoConfigurationProperties;
    }

    public LinkCheckingConfigurationProperties getLinkCheckingConfigurationProperties() {
        return linkCheckingConfigurationProperties;
    }

    public PublishSolrZookeeperConfigurationProperties getPublishSolrZookeeperConfigurationProperties() {
        return publishSolrZookeeperConfigurationProperties;
    }

    public PostgresConfigurationProperties getPostgresConfigurationProperties() {
        return postgresConfigurationProperties;
    }

    public Duration getLinkCheckingMinTimeBetweenSameServerChecks() {
        return Duration.ofMillis(linkCheckingConfigurationProperties.getMinTimeBetweenSameServerChecks());
    }

    @Override
    public final void close() {
        synchronized (this) {
            try {
                if (this.clioPersistenceConnection != null) {
                    this.clioPersistenceConnection.close();
                }
            } finally {
                this.clioPersistenceConnection = null;
            }
        }
    }
}
