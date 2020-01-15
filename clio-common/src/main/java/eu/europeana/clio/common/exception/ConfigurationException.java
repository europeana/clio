package eu.europeana.clio.common.exception;

public class ConfigurationException extends ClioException {

  private static final long serialVersionUID = -1941346631642454891L;

  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
