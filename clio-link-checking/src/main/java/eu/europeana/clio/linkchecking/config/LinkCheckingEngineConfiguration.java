package eu.europeana.clio.linkchecking.config;

import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import eu.europeana.metis.mongo.connection.MongoProperties;
import eu.europeana.metis.solr.connection.SolrProperties;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

public class LinkCheckingEngineConfiguration {

    //Mongo Metis Core
    private String[] mongoCoreHosts;
    private int[] mongoCorePorts;
    private String mongoCoreUsername;
    private String mongoCorePassword;
    private String mongoCoreAuthenticationDatabase;
    private String mongoCoreDatabase;
    private boolean mongoCoreEnableSsl;
    private String mongoCoreApplicationName;

    // Solr/Zookeeper publish
    private String[] publishSolrHosts;
    private String[] publishZookeeperHosts;
    private int[] publishZookeeperPorts;
    private String publishZookeeperChroot;
    private String publishZookeeperDefaultCollection;

    // PostgreSQL
    private String postgresServer;
    private String postgresUsername;
    private String postgresPassword;

    // Link checking
    private Mode linkCheckingMode;
    private int linkCheckingRetentionMonths;
    private int linkCheckingSampleRecordsPerDataset;
    private int linkCheckingRunCreateThreads;
    private int linkCheckingRunExecuteThreads;
    private int linkCheckingMinTimeBetweenSameServerChecks;
    private int linkCheckingConnectTimeout;
    private int linkCheckingResponseTimeout;
    private int linkCheckingDownloadTimeout;

    /**
     * Create a persistence connection provider.
     *
     * @return The connection.
     */
    public ClioPersistenceConnectionProvider getPersistenceConnectionProvider() {
        return new ClioPersistenceConnectionProvider(postgresServer, postgresUsername,
                postgresPassword);
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

    public Mode getLinkCheckingMode() {
        return linkCheckingMode;
    }

    public int getLinkCheckingRetentionMonths() {
        return linkCheckingRetentionMonths;
    }

    public int getLinkCheckingRunCreateThreads() {
        return linkCheckingRunCreateThreads;
    }

    public int getLinkCheckingSampleRecordsPerDataset() {
        return linkCheckingSampleRecordsPerDataset;
    }

    public int getLinkCheckingRunExecuteThreads() {
        return linkCheckingRunExecuteThreads;
    }

    public Duration getLinkCheckingMinTimeBetweenSameServerChecks() {
        return Duration.ofMillis(this.linkCheckingMinTimeBetweenSameServerChecks);
    }

    public String getMongoCoreDatabase() {
        return mongoCoreDatabase;
    }

    public void setMongoCoreHosts(String[] mongoCoreHosts) {
        this.mongoCoreHosts = mongoCoreHosts;
    }

    public void setMongoCorePorts(int[] mongoCorePorts) {
        this.mongoCorePorts = mongoCorePorts;
    }

    public void setMongoCoreUsername(String mongoCoreUsername) {
        this.mongoCoreUsername = mongoCoreUsername;
    }

    public void setMongoCorePassword(String mongoCorePassword) {
        this.mongoCorePassword = mongoCorePassword;
    }

    public void setMongoCoreAuthenticationDatabase(String mongoCoreAuthenticationDatabase) {
        this.mongoCoreAuthenticationDatabase = mongoCoreAuthenticationDatabase;
    }

    public void setMongoCoreDatabase(String mongoCoreDatabase) {
        this.mongoCoreDatabase = mongoCoreDatabase;
    }

    public void setMongoCoreEnableSsl(boolean mongoCoreEnableSsl) {
        this.mongoCoreEnableSsl = mongoCoreEnableSsl;
    }

    public void setMongoCoreApplicationName(String mongoCoreApplicationName) {
        this.mongoCoreApplicationName = mongoCoreApplicationName;
    }

    public void setPublishSolrHosts(String[] publishSolrHosts) {
        this.publishSolrHosts = publishSolrHosts;
    }

    public void setPublishZookeeperHosts(String[] publishZookeeperHosts) {
        this.publishZookeeperHosts = publishZookeeperHosts;
    }

    public void setPublishZookeeperPorts(int[] publishZookeeperPorts) {
        this.publishZookeeperPorts = publishZookeeperPorts;
    }

    public void setPublishZookeeperChroot(String publishZookeeperChroot) {
        this.publishZookeeperChroot = publishZookeeperChroot;
    }

    public void setPublishZookeeperDefaultCollection(String publishZookeeperDefaultCollection) {
        this.publishZookeeperDefaultCollection = publishZookeeperDefaultCollection;
    }

    public void setPostgresServer(String postgresServer) {
        this.postgresServer = postgresServer;
    }

    public void setPostgresUsername(String postgresUsername) {
        this.postgresUsername = postgresUsername;
    }

    public void setPostgresPassword(String postgresPassword) {
        this.postgresPassword = postgresPassword;
    }

    public void setLinkCheckingMode(Mode linkCheckingMode) {
        this.linkCheckingMode = linkCheckingMode;
    }

    public void setLinkCheckingRetentionMonths(int linkCheckingRetentionMonths) {
        this.linkCheckingRetentionMonths = linkCheckingRetentionMonths;
    }

    public void setLinkCheckingSampleRecordsPerDataset(int linkCheckingSampleRecordsPerDataset) {
        this.linkCheckingSampleRecordsPerDataset = linkCheckingSampleRecordsPerDataset;
    }

    public void setLinkCheckingRunCreateThreads(int linkCheckingRunCreateThreads) {
        this.linkCheckingRunCreateThreads = linkCheckingRunCreateThreads;
    }

    public void setLinkCheckingRunExecuteThreads(int linkCheckingRunExecuteThreads) {
        this.linkCheckingRunExecuteThreads = linkCheckingRunExecuteThreads;
    }

    public void setLinkCheckingMinTimeBetweenSameServerChecks(int linkCheckingMinTimeBetweenSameServerChecks) {
        this.linkCheckingMinTimeBetweenSameServerChecks = linkCheckingMinTimeBetweenSameServerChecks;
    }

    public void setLinkCheckingConnectTimeout(int linkCheckingConnectTimeout) {
        this.linkCheckingConnectTimeout = linkCheckingConnectTimeout;
    }

    public void setLinkCheckingResponseTimeout(int linkCheckingResponseTimeout) {
        this.linkCheckingResponseTimeout = linkCheckingResponseTimeout;
    }

    public void setLinkCheckingDownloadTimeout(int linkCheckingDownloadTimeout) {
        this.linkCheckingDownloadTimeout = linkCheckingDownloadTimeout;
    }
}
