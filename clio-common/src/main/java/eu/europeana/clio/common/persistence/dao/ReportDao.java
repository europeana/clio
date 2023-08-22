package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Report;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.ReportRow;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ReportDao {

    private final ClioPersistenceConnection persistenceConnection;

    /**
     * Constructor.
     *
     * @param persistenceConnection The connection to the Clio persistence. Should be connected. This
     *                              object does not close the connection.
     */
    public ReportDao(ClioPersistenceConnection persistenceConnection) {
        this.persistenceConnection = persistenceConnection;
    }

    /**
     * Save a report after last batch.
     *
     * @param report  The report to persist
     * @param batchId The ID of the batch after which the report is relevant
     * @return The ID of the newly created report.
     * @throws PersistenceException In case there was a persistence problem.
     */
    public long saveReport(String report, long batchId) throws PersistenceException {
        return persistenceConnection.performInTransaction(session -> {
            final BatchRow batchRow = session.get(BatchRow.class, batchId);
            if (batchRow == null) {
                throw new PersistenceException(format("Cannot create run: batch with ID %s does not exist.", batchId));
            }
            final ReportRow reportRow = new ReportRow(Instant.now(), report, batchRow);
            return (Long) session.save(reportRow);
        });
    }

    public List<Report> getLatestReports(int maxResults) throws PersistenceException {
        return persistenceConnection.performInSession(session ->
                session.createNamedQuery(ReportRow.GET_LATEST_REPORT_QUERY, ReportRow.class)
                        .setMaxResults(maxResults).getResultList().stream()
                        .map(ReportDao::convert).collect(Collectors.toList()));
    }

    private static Report convert(ReportRow reportRow) {
        return new Report(reportRow.getReportId(), reportRow.getBatch().getBatchId(), reportRow.getCreationTime(), reportRow.getReport());
    }

}
