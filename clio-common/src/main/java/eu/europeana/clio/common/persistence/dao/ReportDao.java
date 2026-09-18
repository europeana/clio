package eu.europeana.clio.common.persistence.dao;

import static java.lang.String.format;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Report;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.ReportRow;
import java.time.Instant;
import java.util.List;
import org.hibernate.SessionFactory;

/**
 * Data access class for accessing reports.
 */
public class ReportDao {

    private final HibernateSessionUtils hibernateSessionUtils;

    /**
     * Constructor.
     *
     * @param sessionFactory The connection to the Clio persistence. Should be connected. This object does not
     * close the connection.
     */
    public ReportDao(SessionFactory sessionFactory) {
        this.hibernateSessionUtils = new HibernateSessionUtils(sessionFactory);
    }

    /**
     * Save a report after last batch.
     *
     * @param report The report to persist
     * @param batchId The ID of the batch after which the report is relevant
     * @return The ID of the newly created report.
     * @throws PersistenceException In case there was a persistence problem.
     */
    public long saveReport(String report, long batchId) throws PersistenceException {
        return hibernateSessionUtils.performInTransaction(session -> {
            final BatchRow batchRow = session.find(BatchRow.class, batchId);
            if (batchRow == null) {
                throw new PersistenceException(format("Cannot create run: batch with ID %s does not exist.", batchId));
            }
            final ReportRow reportRow = new ReportRow(Instant.now(), report, batchRow);
            session.persist(reportRow);
            session.flush();
            return reportRow.getReportId();
        });
    }

    /**
     * Get the latest reports.
     *
     * @param maxResults maximum results to return
     * @return the list of reports
     * @throws PersistenceException in case of a persistence exception
     */
    public List<Report> getLatestReports(int maxResults) throws PersistenceException {
        return hibernateSessionUtils.performInSession(session ->
                session.createNamedQuery(ReportRow.GET_LATEST_REPORT_QUERY, ReportRow.class)
                        .setMaxResults(maxResults).getResultList().stream()
                        .map(ReportDao::convert).toList());
    }

    /**
     * Get all report details.
     *
     * @return the list of report details
     * @throws PersistenceException in case of a persistence exception
     */
    public List<Report> getAllReportDetails() throws PersistenceException {
        return hibernateSessionUtils.performInSession(session ->
                session.createNamedQuery(ReportRow.GET_ALL_REPORT_DETAILS_QUERY, ReportRow.class)
                        .getResultList().stream()
                        .map(ReportDao::convert).toList());
    }

    /**
     * Get a report by its creation time
     *
     * @param batchId the batch id
     * @return the report
     * @throws PersistenceException if there was an error while getting the report
     */
    public Report getReportByBatchId(Long batchId) throws PersistenceException {
        return hibernateSessionUtils.performInSession(session ->
                session.createNamedQuery(ReportRow.GET_REPORT_BY_BATCH_ID_QUERY, ReportRow.class)
                        .setParameter(ReportRow.BATCH_ID_PARAMETER, batchId)
                        .getResultList().stream().map(ReportDao::convert).findFirst().orElse(null));
    }

    /**
     * Gets report by report id.
     *
     * @param reportId the report id
     * @return the report by report id
     * @throws PersistenceException the persistence exception
     */
    public Report getReportByReportId(Long reportId) throws PersistenceException {
        return hibernateSessionUtils.performInSession(session ->
            session.createNamedQuery(ReportRow.GET_REPORT_BY_REPORT_ID_QUERY, ReportRow.class)
                   .setParameter(ReportRow.REPORT_ID_PARAMETER, reportId)
                   .getResultList().stream().map(ReportDao::convert).findFirst().orElse(null));
    }

    private static Report convert(ReportRow reportRow) {
        return new Report(reportRow.getReportId(), reportRow.getBatch().getBatchId(), reportRow.getCreationTime(), reportRow.getReport());
    }

}
