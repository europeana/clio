package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.model.BatchRow;
import java.time.Instant;

/**
 * Data access object for runs (a checking iteration for a given dataset).
 */
public class BatchDao {

  private final ClioPersistenceConnection persistenceConnection;

  /**
   * Constructor.
   *
   * @param persistenceConnection The connection to the Clio persistence. Should be connected. This
   * object does not close the connection.
   */
  public BatchDao(ClioPersistenceConnection persistenceConnection) {
    this.persistenceConnection = persistenceConnection;
  }

  /**
   * Create a new batch with a creation time equal to the current time.
   *
   * @param lastUpdateTimeInSolr The time that the last update was recorded in the Solr.
   * @param lastUpdateTimeInMetsiCore The time that the last update was recorded in the Metis Core
   * Mongo.
   * @return The ID of the newly created batch.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public long createBatchStartingNow(Instant lastUpdateTimeInSolr,
          Instant lastUpdateTimeInMetsiCore) throws PersistenceException {
    return persistenceConnection.performInTransaction(session -> {
      final BatchRow newBatch = new BatchRow(Instant.now(), lastUpdateTimeInSolr,
              lastUpdateTimeInMetsiCore);
      return (Long) session.save(newBatch);
    });
  }

  /**
   * Set/update the counters for the given batch.
   *
   * @param batchId The ID of the batch in which to set/update the counters.
   * @param datasetsExcludedAlreadyRunning The number of datasets excluded because they already have
   * a run in progress.
   * @param datasetsExcludedNotIndexed The number of datasets excluded because they have not yet
   * been indexed.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public void setCountersForBatch(long batchId, int datasetsExcludedAlreadyRunning,
          int datasetsExcludedNotIndexed) throws PersistenceException {
    persistenceConnection.performInTransaction(session -> {
      final BatchRow batchRow = session.get(BatchRow.class, batchId);
      if (batchRow == null) {
        throw new PersistenceException(
                "Cannot set counters: batch with ID " + batchId + " does not exist.");
      }
      batchRow.setCounters(datasetsExcludedAlreadyRunning, datasetsExcludedNotIndexed);
      return null;
    });
  }
}
