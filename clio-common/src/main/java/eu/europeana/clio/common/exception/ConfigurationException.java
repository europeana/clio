package eu.europeana.clio.common.exception;

/**
 * This exception occurs when there is a (probably fatal) problem during configuration and setup.
 */
public class ConfigurationException extends ClioException {

  private static final long serialVersionUID = -1941346631642454891L;

  /**
   * Constructor.
   *
   * @param message The message.
   */
  public ConfigurationException(String message) {
    super(message);
  }

  /**
   * Constructor.
   *
   * @param message The message.
   * @param cause The cause.
   */
  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
