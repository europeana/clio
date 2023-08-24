package eu.europeana.clio.linkchecking.config;

import eu.europeana.clio.common.config.properties.ReportingEngineProperties;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.linkchecking.config.properties.LinkCheckingProperties;
import eu.europeana.clio.linkchecking.execution.LinkCheckingRunner;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import metis.common.config.properties.TruststoreProperties;
import metis.common.config.properties.mongo.MetisCoreMongoProperties;
import metis.common.config.properties.mongo.MongoProperties;
import metis.common.config.properties.postgres.PostgresProperties;
import metis.common.config.properties.solr.PublishSolrZookeeperProperties;
import metis.common.config.properties.solr.SolrZookeeperProperties;
import metis.common.config.properties.solr.ZookeeperProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
@EnableConfigurationProperties({
        LinkCheckingProperties.class, ReportingEngineProperties.class,
        TruststoreProperties.class, PostgresProperties.class,
        MetisCoreMongoProperties.class, PublishSolrZookeeperProperties.class})
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ReportingEngineConfiguration reportingEngineConfiguration;
    private LinkCheckingEngineConfiguration linkCheckingEngineConfiguration;

    /**
     * Autowired constructor for Spring Configuration class.
     *
     * @param truststoreProperties the object that holds all boot configuration values
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException if the configuration of the truststore failed
     */
    @Autowired
    public ApplicationConfiguration(TruststoreProperties truststoreProperties) throws CustomTruststoreAppender.TrustStoreConfigurationException {
        ApplicationConfiguration.initializeApplication(truststoreProperties);
    }

    /**
     * This method performs the initializing tasks for the application.
     *
     * @param propertiesHolder The properties.
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException In case a problem occurred with the truststore.
     */
    static void initializeApplication(TruststoreProperties propertiesHolder)
            throws CustomTruststoreAppender.TrustStoreConfigurationException {

        // Load the trust store file.
        if (StringUtils.isNotEmpty(propertiesHolder.getPath()) && StringUtils
                .isNotEmpty(propertiesHolder.getPassword())) {
            CustomTruststoreAppender
                    .appendCustomTrustoreToDefault(propertiesHolder.getPath(),
                            propertiesHolder.getPassword());
            LOGGER.info("Custom truststore appended to default truststore");
        }
    }

    @Bean
    public ReportingEngineConfiguration getReportingEngineConfiguration(ReportingEngineProperties reportingEngineProperties,
                                                                        PostgresProperties postgresProperties,
                                                                        TruststoreProperties truststoreProperties) throws PersistenceException {
        reportingEngineConfiguration = new ReportingEngineConfiguration();
        reportingEngineConfiguration.setTruststorePath(truststoreProperties.getPath());
        reportingEngineConfiguration.setTruststorePassword(truststoreProperties.getPassword());
        reportingEngineConfiguration.setPostgresServer(postgresProperties.getServer());
        reportingEngineConfiguration.setPostgresUsername(postgresProperties.getUsername());
        reportingEngineConfiguration.setPostgresPassword(postgresProperties.getPassword());
        reportingEngineConfiguration.setReportDatasetLinkTemplate(reportingEngineProperties.getDatasetLinkTemplate());

        LOGGER.info("Found database connection: {}", reportingEngineConfiguration.getPostgresServer());
        final ClioPersistenceConnection persistenceConnection = reportingEngineConfiguration.getClioPersistenceConnection();
        persistenceConnection.verifyConnection();

        return reportingEngineConfiguration;
    }

    @Bean
    public CommandLineRunner commandLineRunner(LinkCheckingProperties linkCheckingProperties,
                                               MongoProperties mongoProperties,
                                               SolrZookeeperProperties solrZookeeperProperties,
                                               PostgresProperties postgresProperties,
                                               ReportingEngineConfiguration reportingEngineConfiguration) throws PersistenceException {
        linkCheckingEngineConfiguration = new LinkCheckingEngineConfiguration();
        linkCheckingEngineConfiguration.setMongoCoreHosts(mongoProperties.getHosts());
        linkCheckingEngineConfiguration.setMongoCorePorts(mongoProperties.getPorts());
        linkCheckingEngineConfiguration.setMongoCoreUsername(mongoProperties.getUsername());
        linkCheckingEngineConfiguration.setMongoCorePassword(mongoProperties.getPassword());
        linkCheckingEngineConfiguration.setMongoCoreAuthenticationDatabase(mongoProperties.getAuthenticationDatabase());
        linkCheckingEngineConfiguration.setMongoCoreDatabase(mongoProperties.getDatabase());
        linkCheckingEngineConfiguration.setMongoCoreEnableSsl(mongoProperties.isEnableSsl());
        linkCheckingEngineConfiguration.setMongoCoreApplicationName(mongoProperties.getApplicationName());

        final ZookeeperProperties zookeeperProperties = solrZookeeperProperties.getZookeeper();
        linkCheckingEngineConfiguration.setPublishSolrHosts(solrZookeeperProperties.getHosts());
        linkCheckingEngineConfiguration.setPublishZookeeperHosts(zookeeperProperties.getHosts());
        linkCheckingEngineConfiguration.setPublishZookeeperPorts(zookeeperProperties.getPorts());
        linkCheckingEngineConfiguration.setPublishZookeeperChroot(zookeeperProperties.getChroot());
        linkCheckingEngineConfiguration.setPublishZookeeperDefaultCollection(zookeeperProperties.getDefaultCollection());

        linkCheckingEngineConfiguration.setPostgresServer(postgresProperties.getServer());
        linkCheckingEngineConfiguration.setPostgresUsername(postgresProperties.getUsername());
        linkCheckingEngineConfiguration.setPostgresPassword(postgresProperties.getPassword());

        linkCheckingEngineConfiguration.setLinkCheckingMode(linkCheckingProperties.getCheckingMode());
        linkCheckingEngineConfiguration.setLinkCheckingRetentionMonths(linkCheckingProperties.getRetentionMonths());
        linkCheckingEngineConfiguration.setLinkCheckingSampleRecordsPerDataset(linkCheckingProperties.getSampleRecordsPerDataset());
        linkCheckingEngineConfiguration.setLinkCheckingRunCreateThreads(linkCheckingProperties.getRunCreateThreads());
        linkCheckingEngineConfiguration.setLinkCheckingRunExecuteThreads(linkCheckingProperties.getRunExecuteThreads());
        linkCheckingEngineConfiguration.setLinkCheckingMinTimeBetweenSameServerChecks(linkCheckingProperties.getMinTimeBetweenSameServerChecks());
        linkCheckingEngineConfiguration.setLinkCheckingConnectTimeout(linkCheckingProperties.getConnectTimeout());
        linkCheckingEngineConfiguration.setLinkCheckingResponseTimeout(linkCheckingProperties.getResponseTimeout());
        linkCheckingEngineConfiguration.setLinkCheckingDownloadTimeout(linkCheckingProperties.getDownloadTimeout());

        LOGGER.info("Found database connection: {}", linkCheckingEngineConfiguration.getPostgresServer());
        final ClioPersistenceConnection persistenceConnection = linkCheckingEngineConfiguration.getClioPersistenceConnection();
        persistenceConnection.verifyConnection();

        return new LinkCheckingRunner(linkCheckingEngineConfiguration, reportingEngineConfiguration);
    }

    /**
     * Closes any connections previous acquired.
     */
    @PreDestroy
    public void close() {
        if (linkCheckingEngineConfiguration != null) {
            linkCheckingEngineConfiguration.close();
        }
        if (reportingEngineConfiguration != null) {
            reportingEngineConfiguration.close();
        }
    }
}
