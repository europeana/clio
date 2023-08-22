package eu.europeana.clio.common.persistence.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "report", indexes = {
        @Index(name = "report_batch_id_idx", columnList = "batch_id")})
@NamedQuery(name = ReportRow.GET_LATEST_REPORT_QUERY,
        query = "SELECT r FROM ReportRow r ORDER BY r.creationTime DESC")
public class ReportRow {

    public static final String GET_LATEST_REPORT_QUERY = "getLatestReport";

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

    public ReportRow(Instant creationTime, String report, BatchRow batch) {
        this.creationTime = creationTime.toEpochMilli();
        this.report = report;
        this.batch = batch;
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
