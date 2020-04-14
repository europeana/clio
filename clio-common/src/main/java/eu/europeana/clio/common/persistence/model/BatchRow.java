package eu.europeana.clio.common.persistence.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * This represents the persistent form of a batch (of runs).
 */
@Entity
@Table(name = "batch")
@NamedQuery(name = BatchRow.GET_LATEST_BATCHES_QUERY,
        query = "SELECT b FROM BatchRow b ORDER BY b.creationTime DESC ")
public class BatchRow {

  public static final String GET_LATEST_BATCHES_QUERY = "getLatestBatches";

  @Id
  @Column(name = "batch_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long batchId;

  @Column(name = "creation_time", nullable = false, updatable = false)
  private long creationTime;

  @Column(name = "last_update_time_solr", nullable = false, updatable = false)
  private long lastUpdateTimeInSolr;

  @Column(name = "last_update_time_metis_core", nullable = false, updatable = false)
  private long lastUpdateTimeInMetisCore;

  @Column(name = "datasets_excluded_already_running")
  private Integer datasetsExcludedAlreadyRunning;

  @Column(name = "datasets_excluded_not_indexed")
  private Integer datasetsExcludedNotIndexed;

  /**
   * Constructor for the use of JPA. Don't use from code.
   */
  protected BatchRow() {
  }

  public BatchRow(Instant creationTime, Instant lastUpdateTimeInSolr,
          Instant lastUpdateTimeInMetisCore) {
    this.creationTime = creationTime.toEpochMilli();
    this.lastUpdateTimeInSolr = lastUpdateTimeInSolr.toEpochMilli();
    this.lastUpdateTimeInMetisCore = lastUpdateTimeInMetisCore.toEpochMilli();
  }

  public void setCounters(int datasetsExcludedAlreadyRunning, int datasetsExcludedNotIndexed) {
    this.datasetsExcludedAlreadyRunning = datasetsExcludedAlreadyRunning;
    this.datasetsExcludedNotIndexed = datasetsExcludedNotIndexed;
  }

  public long getBatchId() {
    return batchId;
  }

  public Instant getCreationTime() {
    return Instant.ofEpochMilli(creationTime);
  }

  public Instant getLastUpdateTimeInSolr() {
    return Instant.ofEpochMilli(lastUpdateTimeInSolr);
  }

  public Instant getLastUpdateTimeInMetisCore() {
    return Instant.ofEpochMilli(lastUpdateTimeInMetisCore);
  }

  public Integer getDatasetsExcludedAlreadyRunning() {
    return datasetsExcludedAlreadyRunning;
  }

  public Integer getDatasetsExcludedNotIndexed() {
    return datasetsExcludedNotIndexed;
  }
}
