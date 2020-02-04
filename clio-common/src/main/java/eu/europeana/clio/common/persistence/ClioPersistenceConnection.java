package eu.europeana.clio.common.persistence;

import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.common.exception.PersistenceException;
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
 * The connection provider to the Clio database. When created, the provider is not connected. The
 * method {@link #connect(String, String, String)} should be called in order for this connection to
 * become usable. Note that this connection should be closed by calling {@link #close()}.
 */
public class ClioPersistenceConnection implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClioPersistenceConnection.class);

  private static Set<Class<?>> annotatedClasses = Set
          .of(DatasetRow.class, RunRow.class, LinkRow.class);

  private SessionFactory sessionFactory;

  /**
   * Establishes a connection. This method can only be called once on any given instance.
   *
   * @param server The server to connect to.
   * @param username The username.
   * @param password The password.
   * @throws ConfigurationException In case there was an issue setting up this connection.
   */
  protected final synchronized void connect(String server, String username, String password)
      throws ConfigurationException {

    if (this.sessionFactory != null) {
      throw new IllegalStateException(
              "A session factory has already been created for this provider.");
    }

    final Configuration config = new Configuration();
    annotatedClasses.forEach(config::addAnnotatedClass);
    config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
    config.setProperty("hibernate.connection.url", server);
    config.setProperty("hibernate.connection.username", username);
    config.setProperty("hibernate.connection.password", password);
    config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    config.setProperty("hibernate.c3p0.timeout", "1800");

    try {
      this.sessionFactory = config.buildSessionFactory();
    } catch (HibernateException e) {
      throw new ConfigurationException("Exception while setting up connection to persistence.", e);
    }
  }

  private SessionFactory getSessionFactory() throws PersistenceException {
    final SessionFactory sessionFactoryToUse = this.sessionFactory;
    if (sessionFactoryToUse == null) {
      throw new PersistenceException(
              "No connection has been established, or the connection has been closed.");
    }
    return sessionFactory;
  }

  /**
   * Perform a persistence action in a session and obtain a {@link Closeable} result.
   *
   * @param action The action to perform.
   * @param <T> The type of the return value.
   * @return The {@link Closeable} result of the action.
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
  public final <T> T performInTransaction(DatabaseAction<T> action)
          throws PersistenceException {
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
  public final synchronized void close() {
    final SessionFactory sessionFactoryToClose = this.sessionFactory;
    this.sessionFactory = null;
    if (sessionFactoryToClose != null) {
      sessionFactoryToClose.close();
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
