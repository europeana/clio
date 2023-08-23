package eu.europeana.clio.linkchecking.config;

import eu.europeana.clio.common.config.properties.PostgresProperties;
import eu.europeana.clio.common.config.properties.ReportingEngineProperties;
import eu.europeana.clio.common.config.properties.TruststoreProperties;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.linkchecking.config.properties.*;
import eu.europeana.clio.linkchecking.execution.LinkCheckingRunner;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
@Import({TruststoreProperties.class, PostgresProperties.class, ReportingEngineProperties.class})
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
        if (StringUtils.isNotEmpty(propertiesHolder.getTruststorePath()) && StringUtils
                .isNotEmpty(propertiesHolder.getTruststorePassword())) {
            CustomTruststoreAppender
                    .appendCustomTrustoreToDefault(propertiesHolder.getTruststorePath(),
                            propertiesHolder.getTruststorePassword());
            LOGGER.info("Custom truststore appended to default truststore");
        }
    }

    @Bean
    public ReportingEngineConfiguration getReportingEngineConfiguration(ReportingEngineProperties reportingEngineProperties,
                                                                        PostgresProperties postgresProperties,
                                                                        TruststoreProperties truststoreProperties) throws PersistenceException {
        reportingEngineConfiguration = new ReportingEngineConfiguration();
        reportingEngineConfiguration.setTruststorePath(truststoreProperties.getTruststorePath());
        reportingEngineConfiguration.setTruststorePassword(truststoreProperties.getTruststorePassword());
        reportingEngineConfiguration.setPostgresServer(postgresProperties.getPostgresServer());
        reportingEngineConfiguration.setPostgresUsername(postgresProperties.getPostgresUsername());
        reportingEngineConfiguration.setPostgresPassword(postgresProperties.getPostgresPassword());
        reportingEngineConfiguration.setReportDatasetLinkTemplate(reportingEngineProperties.getReportDatasetLinkTemplate());

        LOGGER.info("Found database connection: {}", reportingEngineConfiguration.getPostgresServer());
        final ClioPersistenceConnection persistenceConnection = reportingEngineConfiguration.getClioPersistenceConnection();
        persistenceConnection.verifyConnection();

        return reportingEngineConfiguration;
    }

    @Bean
    public CommandLineRunner commandLineRunner(LinkCheckingProperties linkCheckingProperties,
                                               MongoCoreProperties mongoCoreProperties,
                                               SolrZookeeperProperties solrZookeeperProperties,
                                               PostgresProperties postgresProperties,
                                               ReportingEngineConfiguration reportingEngineConfiguration) throws PersistenceException {
        linkCheckingEngineConfiguration = new LinkCheckingEngineConfiguration();
        linkCheckingEngineConfiguration.setMongoCoreHosts(mongoCoreProperties.getMongoCoreHosts());
        linkCheckingEngineConfiguration.setMongoCorePorts(mongoCoreProperties.getMongoCorePorts());
        linkCheckingEngineConfiguration.setMongoCoreUsername(mongoCoreProperties.getMongoCoreUsername());
        linkCheckingEngineConfiguration.setMongoCorePassword(mongoCoreProperties.getMongoCorePassword());
        linkCheckingEngineConfiguration.setMongoCoreAuthenticationDatabase(mongoCoreProperties.getMongoCoreAuthenticationDatabase());
        linkCheckingEngineConfiguration.setMongoCoreDatabase(mongoCoreProperties.getMongoCoreDatabase());
        linkCheckingEngineConfiguration.setMongoCoreEnableSsl(mongoCoreProperties.isMongoCoreEnableSsl());
        linkCheckingEngineConfiguration.setMongoCoreApplicationName(mongoCoreProperties.getMongoCoreApplicationName());

        linkCheckingEngineConfiguration.setPublishSolrHosts(solrZookeeperProperties.getPublishSolrHosts());
        linkCheckingEngineConfiguration.setPublishZookeeperHosts(solrZookeeperProperties.getPublishZookeeperHosts());
        linkCheckingEngineConfiguration.setPublishZookeeperPorts(solrZookeeperProperties.getPublishZookeeperPorts());
        linkCheckingEngineConfiguration.setPublishZookeeperChroot(solrZookeeperProperties.getPublishZookeeperChroot());
        linkCheckingEngineConfiguration.setPublishZookeeperDefaultCollection(solrZookeeperProperties.getPublishZookeeperDefaultCollection());

        linkCheckingEngineConfiguration.setPostgresServer(postgresProperties.getPostgresServer());
        linkCheckingEngineConfiguration.setPostgresUsername(postgresProperties.getPostgresUsername());
        linkCheckingEngineConfiguration.setPostgresPassword(postgresProperties.getPostgresPassword());

        linkCheckingEngineConfiguration.setLinkCheckingMode(linkCheckingProperties.getLinkCheckingMode());
        linkCheckingEngineConfiguration.setLinkCheckingRetentionMonths(linkCheckingProperties.getLinkCheckingRetentionMonths());
        linkCheckingEngineConfiguration.setLinkCheckingSampleRecordsPerDataset(linkCheckingProperties.getLinkCheckingSampleRecordsPerDataset());
        linkCheckingEngineConfiguration.setLinkCheckingRunCreateThreads(linkCheckingProperties.getLinkCheckingRunCreateThreads());
        linkCheckingEngineConfiguration.setLinkCheckingRunExecuteThreads(linkCheckingProperties.getLinkCheckingRunExecuteThreads());
        linkCheckingEngineConfiguration.setLinkCheckingMinTimeBetweenSameServerChecks(linkCheckingProperties.getLinkCheckingMinTimeBetweenSameServerChecks());
        linkCheckingEngineConfiguration.setLinkCheckingConnectTimeout(linkCheckingProperties.getLinkCheckingConnectTimeout());
        linkCheckingEngineConfiguration.setLinkCheckingResponseTimeout(linkCheckingProperties.getLinkCheckingResponseTimeout());
        linkCheckingEngineConfiguration.setLinkCheckingDownloadTimeout(linkCheckingProperties.getLinkCheckingDownloadTimeout());

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
