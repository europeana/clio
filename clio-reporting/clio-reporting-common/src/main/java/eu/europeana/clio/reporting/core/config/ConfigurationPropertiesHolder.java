package eu.europeana.clio.reporting.core.config;

import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Class that is used to read all configuration properties for the application.
 * <p>
 * It uses {@link PropertySource} to identify the properties on application startup
 * </p>
 */
@Component
public class ConfigurationPropertiesHolder {

    // truststore
    @Value("${truststore.path}")
    private String truststorePath;
    @Value("${truststore.password}")
    private String truststorePassword;

    // PostGreSQL
    @Value("${postgresql.server}")
    private String postgresServer;
    @Value("${postgresql.username}")
    private String postgresUsername;
    @Value("${postgresql.password}")
    private String postgresPassword;

    // Reporting
    @Value("${report.dataset.link.template}")
    private String reportDatasetLinkTemplate;

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
