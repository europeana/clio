package eu.europeana.clio.common.persistence;

import eu.europeana.clio.common.exception.ConfigurationException;

/**
 * This is an object that is capable of providing a connection to the Clio persistence.
 */
public class ClioPersistenceConnectionProvider {

  private final String postgresServer;
  private final String postgresUsername;
  private final String postgresPassword;

  /**
   * Constructor.
   * 
   * @param server The server to connect to.
   * @param username The username.
   * @param password The password.
   */
  public ClioPersistenceConnectionProvider(String postgresServer, String postgresUsername,
      String postgresPassword) {
    this.postgresServer = postgresServer;
    this.postgresUsername = postgresUsername;
    this.postgresPassword = postgresPassword;
  }

  /**
   * Create a persistence connection.
   * 
   * @return The connection.
   * @throws ConfigurationException In case the connection could not be created.
   */
  public ClioPersistenceConnection createPersistenceConnection() throws ConfigurationException {
    final ClioPersistenceConnection connectionProvider = new ClioPersistenceConnection();
    connectionProvider.connect(postgresServer, postgresUsername, postgresPassword);
    return connectionProvider;
  }
}
