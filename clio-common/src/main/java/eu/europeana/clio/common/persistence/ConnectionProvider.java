package eu.europeana.clio.common.persistence;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import java.io.Closeable;
import java.util.Set;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionProvider implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProvider.class);

  private static Set<Class<?>> annotatedClasses = Set
          .of(DatasetRow.class, RunRow.class, LinkRow.class);

  private SessionFactory sessionFactory;

  public final synchronized void connect(String server, String username, String password) {

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
    config.setProperty("hibernate.c3p0.timeout","1800");

    this.sessionFactory = config.buildSessionFactory();
  }

  public final <T> T performInSession(DatabaseAction<Session, T> action)
          throws PersistenceException {

    // Get the factory.
    final SessionFactory sessionFactoryToUse = this.sessionFactory;
    if (sessionFactoryToUse == null) {
      throw new PersistenceException(
              "No connection has been established, or the connection has been closed.");
    }

    // Perform the action.
    try (final Session session = sessionFactoryToUse.openSession()) {
      return action.perform(session);
    } catch (RuntimeException e) {
      throw new PersistenceException("Something went wrong accessing persistence.", e);
    }
  }

  public final <T> T performInTransaction(DatabaseAction<Session, T> action)
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

  @FunctionalInterface
  public interface DatabaseAction<I, O> {

    O perform(I input) throws PersistenceException;
  }
}
