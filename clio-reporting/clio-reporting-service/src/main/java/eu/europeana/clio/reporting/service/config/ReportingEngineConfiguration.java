package eu.europeana.clio.reporting.service.config;

import eu.europeana.clio.common.config.properties.ClioConfigurationProperties;
import eu.europeana.clio.reporting.service.ReportingEngine;
import org.hibernate.SessionFactory;

/**
 * Class containing configuration for the {@link ReportingEngine}
 */
public record ReportingEngineConfiguration(
    ClioConfigurationProperties clioConfigurationProperties,
    SessionFactory sessionFactory) {

}
