package eu.europeana.clio.reporting.config;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import eu.europeana.clio.reporting.config.properties.PostgresProperties;
import eu.europeana.clio.reporting.config.properties.ReportingEngineProperties;
import eu.europeana.clio.reporting.config.properties.TruststoreProperties;
import eu.europeana.clio.reporting.runner.ReportingRunner;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ReportingEngineConfiguration reportingEngineConfiguration;

    /**
     * Autowired constructor for Spring Configuration class.
     *
     * @param propertiesHolder the object that holds all boot configuration values
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException if the configuration of the truststore failed
     */
    @Autowired
    public ApplicationConfiguration(TruststoreProperties propertiesHolder) throws CustomTruststoreAppender.TrustStoreConfigurationException {
        ApplicationConfiguration.initializeApplication(propertiesHolder);
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
    public CommandLineRunner commandLineRunner(ReportingEngineProperties reportingEngineProperties,
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


        return new ReportingRunner(reportingEngineConfiguration);
    }

    /**
     * Closes any connections previous acquired.
     */
    @PreDestroy
    public void close() {
        if (reportingEngineConfiguration != null) {
            reportingEngineConfiguration.close();
        }
    }
}
