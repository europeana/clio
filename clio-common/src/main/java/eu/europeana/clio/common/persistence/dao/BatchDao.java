package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.BatchWithCounters;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import org.hibernate.Session;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Data access object for runs (a checking iteration for a given dataset).
 */
public class BatchDao {

    public static final long DAYS_IN_MONTH = 30L;
    private final ClioPersistenceConnection persistenceConnection;

    /**
     * Constructor.
     *
     * @param persistenceConnection The connection to the Clio persistence. Should be connected. This
     *                              object does not close the connection.
     */
    public BatchDao(ClioPersistenceConnection persistenceConnection) {
        this.persistenceConnection = persistenceConnection;
    }

    /**
     * Create a new batch with a creation time equal to the current time.
     *
     * @param lastUpdateTimeInSolr      The time that the last update was recorded in the Solr.
     * @param lastUpdateTimeInMetisCore The time that the last update was recorded in the Metis Core
     *                                  Mongo.
     * @return The ID of the newly created batch.
     * @throws PersistenceException In case there was a persistence problem.
     */
    public long createBatchStartingNow(Instant lastUpdateTimeInSolr,
                                       Instant lastUpdateTimeInMetisCore) throws PersistenceException {
        return persistenceConnection.performInTransaction(session -> {
            final BatchRow newBatch = new BatchRow(Instant.now(), lastUpdateTimeInSolr,
                    lastUpdateTimeInMetisCore);
            return (Long) session.save(newBatch);
        });
    }

    /**
     * Set/update the counters for the given batch.
     *
     * @param batchId                        The ID of the batch in which to set/update the counters.
     * @param datasetsExcludedAlreadyRunning The number of datasets excluded because they already have
     *                                       a run in progress.
     * @param datasetsExcludedNotIndexed     The number of datasets excluded because they have not yet
     *                                       been indexed.
     * @param datasetsExcludedWithoutLinks   The number of datasets excluded because they have no
     *                                       records with links to check.
     * @throws PersistenceException In case there was a persistence problem.
     */
    public void setCountersForBatch(long batchId, int datasetsExcludedAlreadyRunning,
                                    int datasetsExcludedNotIndexed, int datasetsExcludedWithoutLinks)
            throws PersistenceException {
        persistenceConnection.performInTransaction(session -> {
            final BatchRow batchRow = session.get(BatchRow.class, batchId);
            if (batchRow == null) {
                throw new PersistenceException(format("Cannot set counters: batch with ID %s does not exist.", batchId));
            }
            batchRow.setCounters(datasetsExcludedAlreadyRunning, datasetsExcludedNotIndexed,
                    datasetsExcludedWithoutLinks);
            return null;
        });
    }

    /**
     * Compiles information on the latest executed batches.
     *
     * @param maxResults The maximum number of batches to return.
     * @return The batches, in reverse chronological order.
     * @throws PersistenceException In case there was a problem with accessing the data.
     */
    public List<BatchWithCounters> getLatestBatches(int maxResults) throws PersistenceException {
        return persistenceConnection.performInSession(session ->
                session.createNamedQuery(BatchRow.GET_LATEST_BATCHES_QUERY, BatchRow.class)
                        .setMaxResults(maxResults).getResultList().stream()
                        .map(batch -> convert(batch, session)).collect(Collectors.toList()));
    }

    /**
     * Get batches older than the specified numerical months.
     *
     * @param retentionMonths the months value to check with
     * @return the batches older than the specified numerical months
     * @throws PersistenceException In case there was a problem with accessing the data.
     */
    public List<BatchRow> getOlderBatches(int retentionMonths) throws PersistenceException {
        long creationTimeCutOff = Instant.now().minus(retentionMonths * DAYS_IN_MONTH, ChronoUnit.DAYS).toEpochMilli();
        return persistenceConnection.performInSession(session ->
                new ArrayList<>(
                        session.createNamedQuery(BatchRow.GET_OLD_BATCHES_QUERY, BatchRow.class)
                                .setParameter(BatchRow.CREATION_TIME_PARAMETER, creationTimeCutOff)
                                .getResultList()));
    }

    /**
     * Delete batches older than the specified numerical months.
     * <p>Based on the schema cascading might be applicable</p>
     *
     * @param retentionMonths the months value to check with
     * @return the batches older than the specified numerical months
     * @throws PersistenceException In case there was a problem with accessing the data.
     */
    public int deleteOlderBatches(int retentionMonths) throws PersistenceException {
        long creationTimeCutOff = Instant.now().minus(retentionMonths * DAYS_IN_MONTH, ChronoUnit.DAYS).toEpochMilli();
        return persistenceConnection.performInTransaction(session ->
                session.createNamedQuery(BatchRow.DELETE_OLD_BATCHES_QUERY)
                        .setParameter(BatchRow.CREATION_TIME_PARAMETER, creationTimeCutOff)
                        .executeUpdate());
    }

    private static BatchWithCounters convert(BatchRow row, Session session) {

        // First get the total number of runs.
        final long totalRuns = session.createNamedQuery(RunRow.COUNT_RUNS_FOR_BATCH, Long.class)
                .setParameter(RunRow.BATCH_ID_PARAMETER, row.getBatchId()).getSingleResult();

        // Then get the number of pending runs.
        final long pendingRuns = session
                .createNamedQuery(RunRow.COUNT_PENDING_RUNS_FOR_BATCH, Long.class)
                .setParameter(RunRow.BATCH_ID_PARAMETER, row.getBatchId()).getSingleResult();

        // Compute the number of finalized runs. Take care to allow for race conditions: if runs were
        // added in between these queries, assume that no runs are completed. If more runs have become
        // completed in between these queries, don't count them.
        final long completedRuns = Math.max(0, totalRuns - pendingRuns);

        // Convert.
        return new BatchWithCounters(row.getBatchId(), row.getCreationTime(),
                row.getLastUpdateTimeInSolr(), row.getLastUpdateTimeInMetisCore(),
                row.getDatasetsExcludedAlreadyRunning(), row.getDatasetsExcludedNotIndexed(),
                row.getDatasetsExcludedWithoutLinks(), (int) completedRuns, (int) pendingRuns);
    }
}
