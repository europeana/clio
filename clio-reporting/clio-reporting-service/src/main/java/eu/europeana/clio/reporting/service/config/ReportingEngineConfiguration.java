package eu.europeana.clio.reporting.service.config;

import eu.europeana.clio.common.config.properties.ReportingEngineConfigurationProperties;
import org.hibernate.SessionFactory;

/**
 * Class containing configuration for the {@link eu.europeana.clio.reporting.service.ReportingEngine}
 */
public class ReportingEngineConfiguration {
    private final ReportingEngineConfigurationProperties reportingEngineConfigurationProperties;
    private final SessionFactory sessionFactory;

    /**
     * Constructor.
     *
     * @param reportingEngineConfigurationProperties the reporting engine configuration properties
     * @param sessionFactory                         the postgres configuration properties
     */
    public ReportingEngineConfiguration(
            ReportingEngineConfigurationProperties reportingEngineConfigurationProperties, SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.reportingEngineConfigurationProperties = reportingEngineConfigurationProperties;
    }

    public ReportingEngineConfigurationProperties getReportingEngineConfigurationProperties() {
        return reportingEngineConfigurationProperties;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
