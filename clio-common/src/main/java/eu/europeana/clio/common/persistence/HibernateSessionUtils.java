package eu.europeana.clio.common.persistence;

import eu.europeana.clio.common.exception.PersistenceException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.stream.Stream;

/**
 * Hibernate session utilities.
 */
public class HibernateSessionUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateSessionUtils.class);
    private final SessionFactory sessionFactory;

    /**
     * Constructor.
     *
     * @param sessionFactory the session factory.
     */
    public HibernateSessionUtils(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Perform a persistence action in a session and obtain a ({@link Closeable}) result that the
     * caller must be sure to close after use.
     *
     * @param action The action to perform.
     * @param <T>    The type of the return value.
     * @return The ({@link Closeable}) result of the action that the caller must be sure to close
     * after use.
     * @throws PersistenceException In case there was a persistence problem thrown by the action.
     */
    public final <T> StreamResult<T> performForStream(DatabaseAction<Stream<T>> action)
            throws PersistenceException {
        final Session session = sessionFactory.openSession();
        final Stream<T> data = action.perform(session);
        return new StreamResult<>(data, session);
    }

    /**
     * Perform a persistence action in a session.
     *
     * @param action The action to perform.
     * @param <T>    The type of the return value. Can be {@link Void}.
     * @return The result of the action.
     * @throws PersistenceException In case there was a persistence problem thrown by the action.
     */
    public final <T> T performInSession(DatabaseAction<T> action)
            throws PersistenceException {
        try (final Session session = sessionFactory.openSession()) {
            return action.perform(session);
        } catch (RuntimeException e) {
            throw new PersistenceException("Something went wrong accessing persistence.", e);
        }
    }

    /**
     * Perform the persistence action in a transaction (within a session).
     *
     * @param action The action to perform.
     * @param <T>    The type of the return value. Can be {@link Void}.
     * @return The result of the action.
     * @throws PersistenceException In case there was a persistence problem thrown by the action.
     */
    public final <T> T performInTransaction(DatabaseAction<T> action) throws PersistenceException {
        return performInSession(session -> {
            final Transaction transaction = session.beginTransaction();
            try {
                final T result = action.perform(session);
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                try {
                    if (transaction != null && transaction.isActive()) {
                        transaction.rollback();
                    }
                } catch (RuntimeException e1) {
                    LOGGER.warn("Suppressing exception that occurred while rolling back transaction.", e1);
                }
                throw e;
            }
        });
    }

    /**
     * A database action.
     *
     * @param <T> The type of the return value. Can be {@link Void}.
     */
    @FunctionalInterface
    public interface DatabaseAction<T> {

        /**
         * Perform the action.
         *
         * @param session The session in which to perform the action.
         * @return The result of the action.
         * @throws PersistenceException In case there was a persistence problem while performing the
         *                              action.
         */
        T perform(Session session) throws PersistenceException;
    }

}
