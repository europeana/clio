package eu.europeana.clio.common.persistence;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import java.io.Closeable;
import java.util.Set;
import java.util.stream.Stream;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The connection provider to the Clio database. When created, the provider is not connected. It
 * will be connected when the first request is sent. Note that this connection should be closed by
 * calling {@link #close()}.
 */
public class ClioPersistenceConnection implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClioPersistenceConnection.class);

  // TODO JV Have the classes register here, or with annotation, is more transparent.
  private static final Set<Class<?>> annotatedClasses = Set
          .of(DatasetRow.class, BatchRow.class, RunRow.class, LinkRow.class);

  private final String server;
  private final String username;
  private final String password;

  private boolean connected;
  private SessionFactory sessionFactory;

  ClioPersistenceConnection(String server, String username, String password) {
    this.server = server;
    this.username = username;
    this.password = password;
  }

  /**
   * Establishes a connection. This method can only be called once on any given instance.
   *
   * @throws PersistenceException In case there was an issue setting up this connection.
   */
  private void connect()throws PersistenceException {
    synchronized (this) {

      // If a session factory already exists, we cannot do this.
      if (this.connected) {
        throw new IllegalStateException("This connection has already been established.");
      }

      // set connected flag.
      this.connected = true;

      // Set the configuration properties for the connection.
      final Configuration config = new Configuration();
      annotatedClasses.forEach(config::addAnnotatedClass);
      config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
      config.setProperty("hibernate.connection.url", this.server);
      config.setProperty("hibernate.connection.username", this.username);
      config.setProperty("hibernate.connection.password", this.password);
      config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
      config.setProperty("hibernate.c3p0.timeout", "1800");

      // Build the connection.
      try {
        this.sessionFactory = config.buildSessionFactory();
      } catch (HibernateException e) {
        throw new PersistenceException("Exception while setting up connection to persistence.", e);
      }
    }
  }

  private SessionFactory getSessionFactory() throws PersistenceException {
    synchronized (this) {
      if (!this.connected) {
        this.connect();
      }
      if (this.sessionFactory == null) {
        throw new PersistenceException(
                "No connection could be established, or the connection has been closed.");
      }
      return sessionFactory;
    }
  }

  /**
   * Perform a persistence action in a session and obtain a ({@link Closeable}) result that the
   * caller must be sure to close after use.
   *
   * @param action The action to perform.
   * @param <T> The type of the return value.
   * @return The ({@link Closeable}) result of the action that the caller must be sure to close
   * after use.
   * @throws PersistenceException In case there was a persistence problem thrown by the action.
   */
  public final <T> StreamResult<T> performForStream(DatabaseAction<Stream<T>> action)
          throws PersistenceException {
    final Session session = getSessionFactory().openSession();
    final Stream<T> data = action.perform(session);
    return new StreamResult<>(data, session);
  }

  /**
   * Perform a persistence action in a session.
   *
   * @param action The action to perform.
   * @param <T> The type of the return value. Can be {@link Void}.
   * @return The result of the action.
   * @throws PersistenceException In case there was a persistence problem thrown by the action.
   */
  public final <T> T performInSession(DatabaseAction<T> action)
          throws PersistenceException {
    try (final Session session = getSessionFactory().openSession()) {
      return action.perform(session);
    } catch (RuntimeException e) {
      throw new PersistenceException("Something went wrong accessing persistence.", e);
    }
  }

  /**
   * Perform the persistence action in a transaction (within a session).
   *
   * @param action The action to perform.
   * @param <T> The type of the return value. Can be {@link Void}.
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

  @Override
  public final void close() {
    synchronized (this) {
      try {
        if (this.sessionFactory != null) {
          this.sessionFactory.close();
        }
      } finally {
        this.sessionFactory = null;
      }
    }
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
     * action.
     */
    T perform(Session session) throws PersistenceException;
  }

}
