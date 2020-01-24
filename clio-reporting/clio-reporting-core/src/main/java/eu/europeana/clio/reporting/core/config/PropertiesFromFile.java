package eu.europeana.clio.reporting.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

public class PropertiesFromFile extends AbstractPropertiesHolder {

  // truststore
  private final String truststorePath;
  private final String truststorePassword;

  // PostGreSQL
  private final String postgresServer;
  private final String postgresUsername;
  private final String postgresPassword;

  /**
   * Constructor. Reads the property file and loads the properties.
   */
  public PropertiesFromFile(Supplier<InputStream> streamSupplier) {

    // Load properties file.
    final Properties properties = new Properties();
    try (final InputStream stream = streamSupplier.get()) {
      properties.load(stream);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }

    // truststore
    truststorePath = properties.getProperty(TRUSTSTORE_PATH_PROPERTY);
    truststorePassword = properties.getProperty(TRUSTSTORE_PASSWORD_PROPERTY);

    // PostGreSQL
    postgresServer = properties.getProperty(POSTGRES_SERVER_PROPERTY);
    postgresUsername = properties.getProperty(POSTGRES_USERNAME_PROPERTY);
    postgresPassword = properties.getProperty(POSTGRES_PASSWORD_PROPERTY);
  }

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
