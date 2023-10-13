package eu.europeana.clio.reporting.rest.controller.advice;


import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.exception.ReportNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 *{@link ControllerAdvice} class that handles exceptions through spring.
 */
@ControllerAdvice
//@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class GlobalControllerAdvice {

    /**
     * Exception handler.
     *
     * @param exception the exception to handle.
     * @return the handled exception in a response
     */
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(PersistenceException exception) {
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Exception handler.
     *
     * @param exception the exception to handle.
     * @return the handled exception in a response
     */
    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(ReportNotFoundException exception) {
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.NOT_FOUND);
    }

    /**
     * Exception handler.
     *
     * @param exception the exception to handle.
     * @return the handled exception in a response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> exceptionHandler(Exception exception) {
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
