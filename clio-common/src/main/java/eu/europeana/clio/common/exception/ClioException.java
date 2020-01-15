package eu.europeana.clio.common.exception;

public class ClioException extends Exception {

  private static final long serialVersionUID = -8399750549538353832L;

  public ClioException(String message) {
    super(message);
  }

  public ClioException(String message, Throwable cause) {
    super(message, cause);
  }
}
