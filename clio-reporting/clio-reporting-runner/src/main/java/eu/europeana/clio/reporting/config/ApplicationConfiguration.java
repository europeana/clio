package eu.europeana.clio.reporting.config;

import eu.europeana.clio.common.config.properties.ReportingEngineProperties;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import eu.europeana.clio.reporting.runner.ReportingRunner;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import metis.common.config.properties.TruststoreProperties;
import metis.common.config.properties.postgres.PostgresProperties;
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
     * @param truststoreProperties The properties.
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException In case a problem occurred with the truststore.
     */
    static void initializeApplication(TruststoreProperties truststoreProperties)
            throws CustomTruststoreAppender.TrustStoreConfigurationException {

        // Load the trust store file.
        if (StringUtils.isNotEmpty(truststoreProperties.getPath()) && StringUtils
                .isNotEmpty(truststoreProperties.getPassword())) {
            CustomTruststoreAppender
                    .appendCustomTrustoreToDefault(truststoreProperties.getPath(),
                            truststoreProperties.getPassword());
            LOGGER.info("Custom truststore appended to default truststore");
        }
    }

    @Bean
    protected CommandLineRunner commandLineRunner(ReportingEngineProperties reportingEngineProperties,
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
