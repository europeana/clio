package eu.europeana.clio.reporting.rest.controller.advice;

/**
 * Error response class used for exceptions in the REST controller.
 */
public class ErrorResponse {

    private String message;

    /**
     * Constructor.
     *
     * @param message the error message
     */
    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
