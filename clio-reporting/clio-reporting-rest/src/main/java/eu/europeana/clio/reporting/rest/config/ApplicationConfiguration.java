package eu.europeana.clio.reporting.rest.config;

import eu.europeana.clio.common.config.properties.ReportingEngineConfigurationProperties;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.reporting.common.ReportingEngine;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.apm.ElasticAPMConfiguration;
import metis.common.config.properties.TruststoreConfigurationProperties;
import metis.common.config.properties.postgres.PostgresConfigurationProperties;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
@EnableConfigurationProperties({ElasticAPMConfiguration.class, TruststoreConfigurationProperties.class,
        PostgresConfigurationProperties.class, ReportingEngineConfigurationProperties.class})
@ComponentScan(basePackages = {"eu.europeana.clio.reporting.rest.controller"})
public class ApplicationConfiguration implements WebMvcConfigurer {

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
    protected ReportingEngineConfiguration getReportingEngineConfiguration(
            ReportingEngineConfigurationProperties reportingEngineConfigurationProperties,
            PostgresConfigurationProperties postgresConfigurationPropertiess) throws PersistenceException {
        reportingEngineConfiguration = new ReportingEngineConfiguration(reportingEngineConfigurationProperties, postgresConfigurationPropertiess);
        final ClioPersistenceConnection persistenceConnection = reportingEngineConfiguration.getClioPersistenceConnection();
        persistenceConnection.verifyConnection();

        return reportingEngineConfiguration;
    }

    @Bean
    protected ReportingEngine getReportingEngine(ReportingEngineConfiguration reportingEngineConfiguration) {
        return new ReportingEngine(reportingEngineConfiguration);
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
