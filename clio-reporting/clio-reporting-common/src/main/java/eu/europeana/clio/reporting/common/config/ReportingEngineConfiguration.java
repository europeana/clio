package eu.europeana.clio.reporting.common.config;

import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;
import org.springframework.context.annotation.PropertySource;

import java.io.Closeable;

/**
 * Class that is used to read all configuration properties for the application.
 * <p>
 * It uses {@link PropertySource} to identify the properties on application startup
 * </p>
 */
public class ReportingEngineConfiguration implements Closeable {

    // truststore
    private String truststorePath;
    private String truststorePassword;

    // PostGreSQL
    private String postgresServer;
    private String postgresUsername;
    private String postgresPassword;

    // Reporting
    private String reportDatasetLinkTemplate;

    private ClioPersistenceConnection clioPersistenceConnection;

    public ClioPersistenceConnection getClioPersistenceConnection(){
        if(clioPersistenceConnection == null){
            clioPersistenceConnection = new ClioPersistenceConnectionProvider(postgresServer, postgresUsername, postgresPassword)
                    .createPersistenceConnection();
        }
        return clioPersistenceConnection;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public void setPostgresServer(String postgresServer) {
        this.postgresServer = postgresServer;
    }

    public void setPostgresUsername(String postgresUsername) {
        this.postgresUsername = postgresUsername;
    }

    public void setPostgresPassword(String postgresPassword) {
        this.postgresPassword = postgresPassword;
    }

    public void setReportDatasetLinkTemplate(String reportDatasetLinkTemplate) {
        this.reportDatasetLinkTemplate = reportDatasetLinkTemplate;
    }

    public String getPostgresServer() {
        return postgresServer;
    }

    public String getPostgresUsername() {
        return postgresUsername;
    }

    public String getPostgresPassword() {
        return postgresPassword;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public String getReportDatasetLinkTemplate() {
        return reportDatasetLinkTemplate;
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
