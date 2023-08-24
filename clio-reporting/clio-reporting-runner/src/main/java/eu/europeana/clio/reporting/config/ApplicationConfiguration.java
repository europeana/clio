package eu.europeana.clio.reporting.config;

import eu.europeana.clio.common.config.properties.ReportingEngineConfigurationProperties;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import eu.europeana.clio.reporting.runner.ReportingRunner;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import metis.common.config.properties.TruststoreConfigurationProperties;
import metis.common.config.properties.postgres.PostgresConfigurationProperties;
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
        TruststoreConfigurationProperties.class, PostgresConfigurationProperties.class,
        ReportingEngineConfigurationProperties.class})
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private ReportingEngineConfiguration reportingEngineConfiguration;

    /**
     * Autowired constructor for Spring Configuration class.
     *
     * @param truststoreConfigurationProperties the object that holds all boot configuration values
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException if the configuration of the truststore failed
     */
    @Autowired
    public ApplicationConfiguration(TruststoreConfigurationProperties truststoreConfigurationProperties)
            throws CustomTruststoreAppender.TrustStoreConfigurationException {
        ApplicationConfiguration.initializeApplication(truststoreConfigurationProperties);
    }

    /**
     * This method performs the initializing tasks for the application.
     *
     * @param truststoreConfigurationProperties The properties.
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException In case a problem occurred with the truststore.
     */
    static void initializeApplication(TruststoreConfigurationProperties truststoreConfigurationProperties)
            throws CustomTruststoreAppender.TrustStoreConfigurationException {

        // Load the trust store file.
        if (StringUtils.isNotEmpty(truststoreConfigurationProperties.getPath()) && StringUtils
                .isNotEmpty(truststoreConfigurationProperties.getPassword())) {
            CustomTruststoreAppender
                    .appendCustomTrustoreToDefault(truststoreConfigurationProperties.getPath(),
                            truststoreConfigurationProperties.getPassword());
            LOGGER.info("Custom truststore appended to default truststore");
        }
    }

    @Bean
    protected CommandLineRunner commandLineRunner(ReportingEngineConfigurationProperties reportingEngineConfigurationProperties,
                                                  PostgresConfigurationProperties postgresConfigurationProperties) throws PersistenceException {
        reportingEngineConfiguration = new ReportingEngineConfiguration(reportingEngineConfigurationProperties, postgresConfigurationProperties);
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
