package eu.europeana.clio.common.exception;

/**
 * Exception used if a report was not found.
 */
public class ReportNotFoundException extends ClioException {

    private static final long serialVersionUID = -3332292346834265371L;

    /**
     * Constructs a new exception with a default detail message.
     */
    public ReportNotFoundException() {
        super("Report not found");
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public ReportNotFoundException(String message) {
        super(message);
    }
}
