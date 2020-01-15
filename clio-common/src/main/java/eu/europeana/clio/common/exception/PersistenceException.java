package eu.europeana.clio.common.exception;

public class PersistenceException extends Exception {

  private static final long serialVersionUID = -718352205887952579L;

  public PersistenceException(String message) {
    super(message);
  }

  public PersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
