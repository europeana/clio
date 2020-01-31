package eu.europeana.clio.common.exception;

/**
 * This exception occurs when there are problems with persistent data or communicating to the
 * persistence provider.
 */
public class PersistenceException extends ClioException {

  private static final long serialVersionUID = -718352205887952579L;

  /**
   * Constructor.
   *
   * @param message The message.
   */
  public PersistenceException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message The message.
   * @param cause The cause.
   */
  public PersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
