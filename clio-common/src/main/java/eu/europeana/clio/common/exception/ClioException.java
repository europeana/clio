package eu.europeana.clio.common.exception;

/**
 * This is the super class of all exceptions in Clio. It can be instanced directly as well.
 */
public class ClioException extends Exception {

  private static final long serialVersionUID = -8399750549538353832L;

  /**
   * Constructor.
   *
   * @param message The message.
   */
  public ClioException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message The message.
   * @param cause The cause.
   */
  public ClioException(String message, Throwable cause) {
    super(message, cause);
  }
}
