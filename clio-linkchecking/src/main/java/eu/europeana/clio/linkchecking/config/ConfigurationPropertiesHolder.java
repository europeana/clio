package eu.europeana.clio.linkchecking.config;

import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mongo.connection.MongoProperties;
import eu.europeana.metis.solr.connection.SolrProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

/**
 * Class that is used to read all configuration properties for the application.
 * <p>
 * It uses {@link PropertySource} to identify the properties on application startup
 * </p>
 */
@Component
public class ConfigurationPropertiesHolder {

    //Mongo Metis Core
    @Value("${mongo.core.hosts}")
    private String[] mongoCoreHosts;
    @Value("${mongo.core.port}")
    private int[] mongoCorePorts;
    @Value("${mongo.core.username}")
    private String mongoCoreUsername;
    @Value("${mongo.core.password}")
    private String mongoCorePassword;
    @Value("${mongo.core.authentication.db}")
    private String mongoCoreAuthenticationDatabase;
    @Value("${mongo.core.db}")
    private String mongoCoreDatabase;
    @Value("${mongo.core.enable.ssl}")
    private boolean mongoCoreEnableSsl;
    @Value("${mongo.core.application.name}")
    private String mongoCoreApplicationName;

    //Custom truststore
    @Value("${truststore.path}")
    private String truststorePath;
    @Value("${truststore.password}")
    private String truststorePassword;

    // Solr/Zookeeper publish
    @Value("${solr.publish.hosts}")
    private String[] publishSolrHosts;
    @Value("${zookeeper.publish.hosts}")
    private String[] publishZookeeperHosts;
    @Value("${zookeeper.publish.port}")
    private int[] publishZookeeperPorts;
    @Value("${zookeeper.publish.chroot}")
    private String publishZookeeperChroot;
    @Value("${zookeeper.publish.defaultCollection}")
    private String publishZookeeperDefaultCollection;

    // PostGreSQL
    @Value("${postgresql.server}")
    private String postgresServer;
    @Value("${postgresql.username}")
    private String postgresUsername;
    @Value("${postgresql.password}")
    private String postgresPassword;

    // Link checking
    @Value("#{T(eu.europeana.clio.linkchecking.config.Mode).getMode('${linkchecking.mode}')}")
    private Mode linkCheckingMode;
    @Value("${linkchecking.retention.months:6}")
    private int linkCheckingRetentionMonths;
    @Value("${linkchecking.sample.records.per.dataset}")
    private int linkCheckingSampleRecordsPerDataset;
    @Value("${linkchecking.run.create.threads}")
    private int linkCheckingRunCreateThreads;
    @Value("${linkchecking.run.execute.threads}")
    private int linkCheckingRunExecuteThreads;
    @Value("${linkchecking.min.time.between.same.server.checks}")
    private int linkCheckingMinTimeBetweenSameServerChecks;
    @Value("${linkchecking.connect.timeout}")
    private int linkCheckingConnectTimeout;
    @Value("${linkchecking.response.timeout}")
    private int linkCheckingResponseTimeout;
    @Value("${linkchecking.download.timeout}")
    private int linkCheckingDownloadTimeout;


    public String getTruststorePath() {
        return truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public String[] getMongoCoreHosts() {
        return mongoCoreHosts;
    }

    public int[] getMongoCorePorts() {
        return mongoCorePorts;
    }

    public String getMongoCoreUsername() {
        return mongoCoreUsername;
    }

    public String getMongoCorePassword() {
        return mongoCorePassword;
    }

    public String getMongoCoreAuthenticationDatabase() {
        return mongoCoreAuthenticationDatabase;
    }

    public boolean isMongoCoreEnableSsl() {
        return mongoCoreEnableSsl;
    }

    public String getMongoCoreApplicationName() {
        return mongoCoreApplicationName;
    }

    public String[] getPublishSolrHosts() {
        return publishSolrHosts;
    }

    public String[] getPublishZookeeperHosts() {
        return publishZookeeperHosts;
    }

    public int[] getPublishZookeeperPorts() {
        return publishZookeeperPorts;
    }

    public String getPublishZookeeperChroot() {
        return publishZookeeperChroot;
    }

    public String getPublishZookeeperDefaultCollection() {
        return publishZookeeperDefaultCollection;
    }

    public String getPostgresServer() {
        return postgresServer;
    }

    public String getPostgresUsername() {
        return postgresUsername;
    }

    public String getPostgresPassword() {
        return postgresPassword;
    }

    public Mode getLinkCheckingMode() {
        return linkCheckingMode;
    }

    public int getLinkCheckingRetentionMonths() {
        return linkCheckingRetentionMonths;
    }

    public int getLinkCheckingSampleRecordsPerDataset() {
        return linkCheckingSampleRecordsPerDataset;
    }

    public int getLinkCheckingRunCreateThreads() {
        return linkCheckingRunCreateThreads;
    }

    public int getLinkCheckingRunExecuteThreads() {
        return linkCheckingRunExecuteThreads;
    }

    public int getLinkCheckingConnectTimeout() {
        return linkCheckingConnectTimeout;
    }

    public int getLinkCheckingResponseTimeout() {
        return linkCheckingResponseTimeout;
    }

    public int getLinkCheckingDownloadTimeout() {
        return linkCheckingDownloadTimeout;
    }

    public MongoProperties<ConfigurationException> getMongoProperties()
            throws ConfigurationException {
        final MongoProperties<ConfigurationException> properties =
                new MongoProperties<>(ConfigurationException::new);
        properties.setAllProperties(mongoCoreHosts, mongoCorePorts, mongoCoreAuthenticationDatabase,
                mongoCoreUsername, mongoCorePassword, mongoCoreEnableSsl, null, mongoCoreApplicationName);
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

    /**
     * Create a persistence connection provider.
     *
     * @return The connection.
     */
    public ClioPersistenceConnectionProvider getPersistenceConnectionProvider() {
        return new ClioPersistenceConnectionProvider(postgresServer, postgresUsername,
                postgresPassword);
    }

    public Duration getLinkCheckingMinTimeBetweenSameServerChecks() {
        return Duration.ofMillis(this.linkCheckingMinTimeBetweenSameServerChecks);
    }

    /**
     * Create a link checker object.
     *
     * @return A link checker object.
     * @throws ConfigurationException In case the link checker could not be created.
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

    public String getMongoCoreDatabase() {
        return mongoCoreDatabase;
    }
}
