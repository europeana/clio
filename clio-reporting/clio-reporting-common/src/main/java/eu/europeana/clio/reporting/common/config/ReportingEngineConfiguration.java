package eu.europeana.clio.reporting.common.config;

import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;
import org.springframework.context.annotation.PropertySource;

/**
 * Class that is used to read all configuration properties for the application.
 * <p>
 * It uses {@link PropertySource} to identify the properties on application startup
 * </p>
 */
public class ReportingEngineConfiguration {

    // truststore
    private String truststorePath;
    private String truststorePassword;

    // PostGreSQL
    private String postgresServer;
    private String postgresUsername;
    private String postgresPassword;

    // Reporting
    private String reportDatasetLinkTemplate;

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

    public ClioPersistenceConnectionProvider getPersistenceConnectionProvider() {
        return new ClioPersistenceConnectionProvider(getPostgresServer(), getPostgresUsername(),
                getPostgresPassword());
    }
}
