package eu.europeana.clio.common.persistence;

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
   * @param postgresServer The server to connect to.
   * @param postgresUsername The username.
   * @param postgresPassword The password.
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
   */
  public ClioPersistenceConnection createPersistenceConnection() {
    return new ClioPersistenceConnection(postgresServer, postgresUsername, postgresPassword);
  }
}
