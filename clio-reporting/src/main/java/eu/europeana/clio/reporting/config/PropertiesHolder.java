package eu.europeana.clio.reporting.config;

import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Contains the properties required for the execution of the link checking functionality.
 */
public class PropertiesHolder {

  private static final String CONFIGURATION_FILE = "application.properties";

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
  public PropertiesHolder() {

    // Load properties file.
    final Properties properties = new Properties();
    try (final InputStream stream = PropertiesHolder.class.getClassLoader()
            .getResourceAsStream(CONFIGURATION_FILE)) {
      properties.load(stream);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }

    // truststore
    truststorePath = properties.getProperty("truststore.path");
    truststorePassword = properties.getProperty("truststore.password");

    // PostGreSQL
    postgresServer = properties.getProperty("postgresql.server");
    postgresUsername = properties.getProperty("postgresql.username");
    postgresPassword = properties.getProperty("postgresql.password");
  }

  public String getTruststorePath() {
    return truststorePath;
  }

  public String getTruststorePassword() {
    return truststorePassword;
  }

  /**
   * Create a connected persistence connection.
   *
   * @return The connection.
   */
  public ClioPersistenceConnection createPersistenceConnection() {
    final ClioPersistenceConnection connectionProvider = new ClioPersistenceConnection();
    connectionProvider.connect(postgresServer, postgresUsername, postgresPassword);
    return connectionProvider;
  }
}
