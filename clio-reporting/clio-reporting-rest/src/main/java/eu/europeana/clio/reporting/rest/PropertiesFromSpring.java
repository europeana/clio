package eu.europeana.clio.reporting.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import eu.europeana.clio.reporting.core.config.AbstractPropertiesHolder;

@Component
@PropertySource({"classpath:application.properties"})
public class PropertiesFromSpring extends AbstractPropertiesHolder {

  // truststore
  @Value("${" + TRUSTSTORE_PATH_PROPERTY + "}")
  private String truststorePath;
  @Value("${" + TRUSTSTORE_PASSWORD_PROPERTY + "}")
  private String truststorePassword;

  // PostGreSQL
  @Value("${" + POSTGRES_SERVER_PROPERTY + "}")
  private String postgresServer;
  @Value("${" + POSTGRES_USERNAME_PROPERTY + "}")
  private String postgresUsername;
  @Value("${" + POSTGRES_PASSWORD_PROPERTY + "}")
  private String postgresPassword;

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
}
