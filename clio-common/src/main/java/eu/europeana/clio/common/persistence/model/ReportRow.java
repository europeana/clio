package eu.europeana.clio.common.persistence.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Database {@link Entity} for a report.
 */
@Entity
@Table(name = "report", indexes = {
    @Index(name = "report_batch_id_idx", columnList = "batch_id")})
@NamedQuery(name = ReportRow.GET_LATEST_REPORT_QUERY,
    query = "SELECT r FROM ReportRow r ORDER BY r.creationTime DESC")
@NamedQuery(query = "SELECT new ReportRow(r.reportId, r.batch, r.creationTime) FROM ReportRow r ORDER BY r.creationTime DESC",
    name = ReportRow.GET_ALL_REPORT_DETAILS_QUERY)
@NamedQuery(name = ReportRow.GET_REPORT_BY_BATCH_ID_QUERY,
    query = "FROM ReportRow AS r WHERE r.batch.batchId = :" + ReportRow.BATCH_ID_PARAMETER)
public class ReportRow {

  public static final String GET_LATEST_REPORT_QUERY = "getLatestReport";
  public static final String GET_ALL_REPORT_DETAILS_QUERY = "getAllReportDetails";
  public static final String GET_REPORT_BY_BATCH_ID_QUERY = "getReportByBatchId";
  public static final String BATCH_ID_PARAMETER = "batchId";

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
   *
   * @param reportId the report id
   * @param batch the batch
   * @param creationTime the creation time
   */
  public ReportRow(long reportId, BatchRow batch, long creationTime) {
    this.reportId = reportId;
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
