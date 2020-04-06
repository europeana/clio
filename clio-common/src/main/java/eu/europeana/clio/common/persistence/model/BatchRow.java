package eu.europeana.clio.common.persistence.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * This represents the persistent form of a batch (of runs).
 */
@Entity
@Table(name = "batch")
public class BatchRow {

  @Id
  @Column(name = "batch_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long batchId;

  @Column(name = "creation_time", nullable = false, updatable = false)
  private long creationTime;

  @Column(name = "last_update_time_solr", nullable = false, updatable = false)
  private long lastUpdateTimeInSolr;

  @Column(name = "last_update_time_metis_core", nullable = false, updatable = false)
  private long lastUpdateTimeInMetsiCore;

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
          Instant lastUpdateTimeInMetsiCore) {
    this.creationTime = creationTime.toEpochMilli();
    this.lastUpdateTimeInSolr = lastUpdateTimeInSolr.toEpochMilli();
    this.lastUpdateTimeInMetsiCore = lastUpdateTimeInMetsiCore.toEpochMilli();
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

  public Instant getLastUpdateTimeInMetsiCore() {
    return Instant.ofEpochMilli(lastUpdateTimeInMetsiCore);
  }

  public int getDatasetsExcludedAlreadyRunning() {
    return datasetsExcludedAlreadyRunning;
  }

  public int getDatasetsExcludedNotIndexed() {
    return datasetsExcludedNotIndexed;
  }
}
