package eu.europeana.clio.reporting.common.config;

import eu.europeana.clio.common.config.properties.ReportingEngineConfigurationProperties;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;
import metis.common.config.properties.postgres.PostgresConfigurationProperties;

import java.io.Closeable;

/**
 * Class containing configuration for the {@link eu.europeana.clio.reporting.common.ReportingEngine}
 */
public class ReportingEngineConfiguration implements Closeable {
    private final PostgresConfigurationProperties postgresConfigurationProperties;
    private final ReportingEngineConfigurationProperties reportingEngineConfigurationProperties;

    private ClioPersistenceConnection clioPersistenceConnection;

    /**
     * Constructor.
     *
     * @param reportingEngineConfigurationProperties the reporting engine configuration properties
     * @param postgresConfigurationProperties        the postgres configuration properties
     */
    public ReportingEngineConfiguration(
            ReportingEngineConfigurationProperties reportingEngineConfigurationProperties, PostgresConfigurationProperties postgresConfigurationProperties) {
        this.postgresConfigurationProperties = postgresConfigurationProperties;
        this.reportingEngineConfigurationProperties = reportingEngineConfigurationProperties;
    }

    public ClioPersistenceConnection getClioPersistenceConnection(){
        if(clioPersistenceConnection == null){
            clioPersistenceConnection = new ClioPersistenceConnectionProvider(
                    postgresConfigurationProperties.getServer(),
                    postgresConfigurationProperties.getUsername(),
                    postgresConfigurationProperties.getPassword())
                    .createPersistenceConnection();
        }
        return clioPersistenceConnection;
    }

    public ReportingEngineConfigurationProperties getReportingEngineConfigurationProperties() {
        return reportingEngineConfigurationProperties;
    }

    @Override
    public final void close() {
        synchronized (this) {
            try {
                if (this.clioPersistenceConnection != null) {
                    this.clioPersistenceConnection.close();
                }
            } finally {
                this.clioPersistenceConnection = null;
            }
        }
    }
}
