package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import java.time.Instant;

/**
 * Data access object for runs (a checking iteration for a given dataset).
 */
public class RunDao {

  private final ClioPersistenceConnection persistenceConnection;

  /**
   * Constructor.
   *
   * @param persistenceConnection The connection to the Clio persistence. Should be connected. This
   * object does not close the connection.
   */
  public RunDao(ClioPersistenceConnection persistenceConnection) {
    this.persistenceConnection = persistenceConnection;
  }

  /**
   * Create a run with a starting time equal to the current time.
   *
   * @param datasetId The (Metis) dataset ID of the dataset to which this run belongs.
   * @return The ID of the run.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public long createRunStartingNow(String datasetId) throws PersistenceException {
    return persistenceConnection.performInTransaction(session -> {
      final DatasetRow datasetRow = session.get(DatasetRow.class, datasetId);
      if (datasetRow == null) {
        throw new PersistenceException(
                "Cannot create run: dataset with ID " + datasetId + " does not exist.");
      }
      final RunRow newRun = new RunRow(System.currentTimeMillis(), datasetRow);
      return (Long) session.save(newRun);
    });
  }

  /**
   * Determines whether the dataset in question currently has an active run (i.e. a run for which at
   * least one link has not been checked yet).
   *
   * @param datasetId The dataset ID for which to check.
   * @return Whether there is an active run.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public boolean datasetHasActiveRun(String datasetId) throws PersistenceException {
    return persistenceConnection.performInSession(
            session -> !session.createNamedQuery(RunRow.GET_ACTIVE_RUN_FOR_DATASET)
                    .setParameter(RunRow.DATASET_ID_PARAMETER, datasetId).getResultList()
                    .isEmpty());
  }

  static Run convert(RunRow row) {
    return new Run(row.getRunId(), Instant.ofEpochMilli(row.getStartingTime()),
            DatasetDao.convert(row.getDataset()));
  }
}
