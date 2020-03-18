package eu.europeana.clio.reporting.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import eu.europeana.clio.reporting.core.config.AbstractPropertiesHolder;

/**
 * A version of the {@link AbstractPropertiesHolder} that gets the property values from Spring
 * injection/property loading.
 */
@Component
@PropertySource({"classpath:application.properties"})
public class PropertiesFromSpring extends AbstractPropertiesHolder {

  // truststore
  @Value("${" + TRUSTSTORE_PATH_PROPERTY + "}")
  private String truststorePath;
  @Value("${" + TRUSTSTORE_PASS_PROPERTY + "}")
  private String truststorePassword;

  // PostGreSQL
  @Value("${" + POSTGRES_SERVER_PROPERTY + "}")
  private String postgresServer;
  @Value("${" + POSTGRES_USERNAME_PROPERTY + "}")
  private String postgresUsername;
  @Value("${" + POSTGRES_PASS_PROPERTY + "}")
  private String postgresPassword;

  // Reporting
  @Value("${" + REPORT_DATASET_LINK_TEMPLATE_PROPERTY + "}")
  private String reportDatasetLinkTemplate;

  @Override
  protected String getPostgresServer() {
    return postgresServer;
  }

  @Override
  protected String getPostgresUsername() {
    return postgresUsername;
  }

  @Override
  protected String getPostgresPassword() {
    return postgresPassword;
  }

  @Override
  public String getTruststorePath() {
    return truststorePath;
  }

  @Override
  public String getTruststorePassword() {
    return truststorePassword;
  }

  @Override
  public String getReportDatasetLinkTemplate() {
    return reportDatasetLinkTemplate;
  }
}
