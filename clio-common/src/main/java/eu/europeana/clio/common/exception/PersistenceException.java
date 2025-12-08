package eu.europeana.clio.common.exception;

import lombok.experimental.StandardException;

/**
 * This exception occurs when there are problems with persistent data or communicating to the persistence provider.
 */
@StandardException
public class PersistenceException extends ClioException {

}
