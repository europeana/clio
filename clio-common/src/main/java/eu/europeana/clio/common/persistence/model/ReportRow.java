package eu.europeana.clio.common.persistence.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.Instant;

/**
 * Database {@link Entity} for a report.
 */
@Entity
@Table(name = "report", indexes = {
        @Index(name = "report_batch_id_idx", columnList = "batch_id")})
@NamedQuery(name = ReportRow.GET_LATEST_REPORT_QUERY,
        query = "SELECT r FROM ReportRow r ORDER BY r.creationTime DESC")
@NamedQuery(query = "SELECT new ReportRow(r.batch, r.creationTime) FROM ReportRow r ORDER BY r.creationTime DESC",
        name = ReportRow.GET_ALL_REPORT_DETAILS_QUERY)
@NamedQuery(name = ReportRow.GET_REPORT_BY_DATE_QUERY,
        query = "FROM ReportRow AS r WHERE r.creationTime = :" + ReportRow.CREATION_DATE_PARAMETER)
public class ReportRow {

    public static final String GET_LATEST_REPORT_QUERY = "getLatestReport";
    public static final String GET_ALL_REPORT_DETAILS_QUERY = "getAllReportDetails";
    public static final String GET_REPORT_BY_DATE_QUERY = "getReportByDate";
    public static final String CREATION_DATE_PARAMETER = "creationTime";

    @Id
    @Column(name = "report_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long reportId;

    @Column(name = "creation_time", nullable = false, updatable = false)
    private long creationTime;

    @Lob
    @Column(name = "report", nullable = false, updatable = false)
    private String report;

    @OneToOne
    @JoinColumn(name = "batch_id", referencedColumnName = "batch_id",
            foreignKey = @ForeignKey(name = "report_batch_id_fkey"), nullable = false, updatable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BatchRow batch;

    public ReportRow() {
    }

    /**
     * Constructor.
     *
     * @param creationTime the creation time
     * @param report the report in String format
     * @param batch the matching batch
     */
    public ReportRow(Instant creationTime, String report, BatchRow batch) {
        this.creationTime = creationTime.toEpochMilli();
        this.report = report;
        this.batch = batch;
    }

    /**
     * Used for an optimized named query
     * @param batch the batch
     * @param creationTime the creation time
     */
    public ReportRow(BatchRow batch, long creationTime) {
        this.batch = batch;
        this.creationTime = creationTime;
    }

    public long getReportId() {
        return reportId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getReport() {
        return report;
    }

    public BatchRow getBatch() {
        return batch;
    }
}
