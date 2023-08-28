package eu.europeana.clio.reporting.runner.config;

import eu.europeana.clio.common.config.properties.ReportingEngineConfigurationProperties;
import eu.europeana.clio.common.persistence.model.*;
import eu.europeana.clio.reporting.service.config.ReportingEngineConfiguration;
import eu.europeana.clio.reporting.runner.execution.ReportingRunner;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import metis.common.config.properties.TruststoreConfigurationProperties;
import metis.common.config.properties.postgres.HibernateConfigurationProperties;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
@EnableConfigurationProperties({
        TruststoreConfigurationProperties.class, HibernateConfigurationProperties.class,
        ReportingEngineConfigurationProperties.class})
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private SessionFactory sessionFactory;

    /**
     * Autowired constructor for Spring Configuration class.
     *
     * @param truststoreConfigurationProperties the object that holds all boot configuration values
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException if the configuration of the truststore failed
     */
    @Autowired
    public ApplicationConfiguration(TruststoreConfigurationProperties truststoreConfigurationProperties)
            throws CustomTruststoreAppender.TrustStoreConfigurationException {
        ApplicationConfiguration.initializeTruststore(truststoreConfigurationProperties);
    }

    /**
     * Truststore initializer
     *
     * @param truststoreConfigurationProperties the truststore configuration properties
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException In case a problem occurred with the truststore.
     */
    static void initializeTruststore(TruststoreConfigurationProperties truststoreConfigurationProperties)
            throws CustomTruststoreAppender.TrustStoreConfigurationException {
        if (StringUtils.isNotEmpty(truststoreConfigurationProperties.getPath()) && StringUtils
                .isNotEmpty(truststoreConfigurationProperties.getPassword())) {
            CustomTruststoreAppender.appendCustomTruststoreToDefault(truststoreConfigurationProperties.getPath(),
                            truststoreConfigurationProperties.getPassword());
            LOGGER.info("Custom truststore appended to default truststore");
        }
    }

    /**
     * Get the session factory.
     *
     * @param hibernateConfigurationProperties the hibernate configuration properties
     * @return the session factory
     * @throws IOException if an I/O error occurs during sql script initialization
     */
    @Bean
    public SessionFactory getSessionFactory(HibernateConfigurationProperties hibernateConfigurationProperties) throws IOException {

        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
        configuration.addAnnotatedClass(DatasetRow.class);
        configuration.addAnnotatedClass(BatchRow.class);
        configuration.addAnnotatedClass(RunRow.class);
        configuration.addAnnotatedClass(LinkRow.class);
        configuration.addAnnotatedClass(ReportRow.class);

        //Apply code configuration to allow spring boot to handle the properties injection
        configuration.setProperty("hibernate.connection.driver_class",
                hibernateConfigurationProperties.getConnection().getDriverClass());
        configuration.setProperty("hibernate.connection.url", hibernateConfigurationProperties.getConnection().getUrl());
        configuration.setProperty("hibernate.connection.username", hibernateConfigurationProperties.getConnection().getUsername());
        configuration.setProperty("hibernate.connection.password", hibernateConfigurationProperties.getConnection().getPassword());
        configuration.setProperty("hibernate.dialect", hibernateConfigurationProperties.getDialect());
        configuration.setProperty("hibernate.c3p0.min_size", hibernateConfigurationProperties.getC3p0().getMinSize());
        configuration.setProperty("hibernate.c3p0.max_size", hibernateConfigurationProperties.getC3p0().getMaxSize());
        configuration.setProperty("hibernate.c3p0.timeout", hibernateConfigurationProperties.getC3p0().getTimeout());
        configuration.setProperty("hibernate.c3p0.max_statements", hibernateConfigurationProperties.getC3p0().getMaxStatements());
        configuration.setProperty("hibernate.hbm2ddl.auto", hibernateConfigurationProperties.getHbm2ddl().getAuto());

        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties()).build();
        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        return sessionFactory;
    }

    @Bean
    protected CommandLineRunner commandLineRunner(ReportingEngineConfigurationProperties reportingEngineConfigurationProperties,
                                                  SessionFactory sessionFactory) {
        final ReportingEngineConfiguration reportingEngineConfiguration =
                new ReportingEngineConfiguration(reportingEngineConfigurationProperties, sessionFactory);
        return new ReportingRunner(reportingEngineConfiguration);
    }

    /**
     * Closes connections to databases when the application stops.
     */
    @PreDestroy
    public void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}
