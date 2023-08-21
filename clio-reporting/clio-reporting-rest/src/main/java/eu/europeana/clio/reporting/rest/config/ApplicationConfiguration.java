package eu.europeana.clio.reporting.rest.config;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.reporting.common.ReportingEngine;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import eu.europeana.clio.reporting.rest.config.properties.PostgresProperties;
import eu.europeana.clio.reporting.rest.config.properties.ReportingEngineProperties;
import eu.europeana.clio.reporting.rest.config.properties.TruststoreProperties;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.apm.ElasticAPMConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.invoke.MethodHandles;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
@Import({ElasticAPMConfiguration.class})
@ComponentScan(basePackages = {"eu.europeana.clio.reporting.rest.controller"})
public class ApplicationConfiguration implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    /**
     * Autowired constructor for Spring Configuration class.
     *
     * @param truststoreProperties the object that holds all boot configuration values
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException if the configuration of the truststore failed
     */
    @Autowired
    public ApplicationConfiguration(TruststoreProperties truststoreProperties)
            throws CustomTruststoreAppender.TrustStoreConfigurationException {
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .resourceChain(false);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui/index.html");
    }

    @Bean
    public ReportingEngineConfiguration getReportingEngineConfiguration(ReportingEngineProperties reportingEngineProperties,
                                                                        PostgresProperties postgresProperties,
                                                                        TruststoreProperties truststoreProperties) throws PersistenceException {
        final ReportingEngineConfiguration reportingEngineConfiguration = new ReportingEngineConfiguration();
        reportingEngineConfiguration.setTruststorePath(truststoreProperties.getTruststorePath());
        reportingEngineConfiguration.setTruststorePassword(truststoreProperties.getTruststorePassword());
        reportingEngineConfiguration.setPostgresServer(postgresProperties.getPostgresServer());
        reportingEngineConfiguration.setPostgresUsername(postgresProperties.getPostgresUsername());
        reportingEngineConfiguration.setPostgresPassword(postgresProperties.getPostgresPassword());
        reportingEngineConfiguration.setReportDatasetLinkTemplate(reportingEngineProperties.getReportDatasetLinkTemplate());

        LOGGER.info("Found database connection: {}", reportingEngineConfiguration.getPostgresServer());
        try (final ClioPersistenceConnection persistenceConnection =
                     reportingEngineConfiguration.getPersistenceConnectionProvider().createPersistenceConnection()) {
            persistenceConnection.verifyConnection();
        }

        return reportingEngineConfiguration;
    }

    @Bean
    public ReportingEngine getReportingEngine(ReportingEngineConfiguration reportingEngineConfiguration) {
        return new ReportingEngine(reportingEngineConfiguration);
    }
}
