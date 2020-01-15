package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ConnectionProvider;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.RunRow;

public class RunDao {

  private final ConnectionProvider connectionProvider;

  public RunDao(ConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  public long createRunStartingNow(String datasetId) throws PersistenceException {
    return connectionProvider.performInTransaction(session -> {
      final DatasetRow datasetRow = session.get(DatasetRow.class, datasetId);
      if (datasetRow == null) {
        throw new PersistenceException(
                "Cannot create run: dataset with ID " + datasetId + " does not exist.");
      }
      final RunRow newRun = new RunRow(System.currentTimeMillis(), datasetRow);
      return (long) session.save(newRun);
    });
  }
}
