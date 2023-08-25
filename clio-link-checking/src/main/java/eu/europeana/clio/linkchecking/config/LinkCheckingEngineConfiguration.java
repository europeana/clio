package eu.europeana.clio.linkchecking.config;

import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.linkchecking.config.properties.LinkCheckingConfigurationProperties;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mongo.connection.MongoProperties;
import eu.europeana.metis.solr.connection.SolrProperties;
import metis.common.config.properties.mongo.MetisCoreMongoConfigurationProperties;
import metis.common.config.properties.solr.PublishSolrZookeeperConfigurationProperties;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

/**
 * Class containing configuration for the {@link eu.europeana.clio.linkchecking.execution.LinkCheckingEngine}
 */
public class LinkCheckingEngineConfiguration {

    private final LinkCheckingConfigurationProperties linkCheckingConfigurationProperties;
    private final MetisCoreMongoConfigurationProperties metisCoreMongoConfigurationProperties;
    private final PublishSolrZookeeperConfigurationProperties publishSolrZookeeperConfigurationProperties;
    private final SessionFactory sessionFactory;

    /**
     * Constructor.
     *
     * @param linkCheckingConfigurationProperties the link checking configuration properties
     * @param metisCoreMongoConfigurationProperties the metis core mongo configuration properties
     * @param publishSolrZookeeperConfigurationProperties the publish solr zookeeper configuration properties
     * @param sessionFactory the postgres configuration properties
     */
    public LinkCheckingEngineConfiguration(LinkCheckingConfigurationProperties linkCheckingConfigurationProperties,
                                           MetisCoreMongoConfigurationProperties metisCoreMongoConfigurationProperties,
                                           PublishSolrZookeeperConfigurationProperties publishSolrZookeeperConfigurationProperties,
                                           SessionFactory sessionFactory) {
        this.linkCheckingConfigurationProperties = linkCheckingConfigurationProperties;
        this.metisCoreMongoConfigurationProperties = metisCoreMongoConfigurationProperties;
        this.publishSolrZookeeperConfigurationProperties = publishSolrZookeeperConfigurationProperties;
        this.sessionFactory = sessionFactory;
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
                publishSolrZookeeperConfigurationProperties.getZookeeper().getHosts(), publishSolrZookeeperConfigurationProperties.getZookeeper().getPorts());
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

    public Duration getLinkCheckingMinTimeBetweenSameServerChecks() {
        return Duration.ofMillis(linkCheckingConfigurationProperties.getMinTimeBetweenSameServerChecks());
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
