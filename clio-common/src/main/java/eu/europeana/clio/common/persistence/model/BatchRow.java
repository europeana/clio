package eu.europeana.clio.common.persistence.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * This represents the persistent form of a batch (of runs).
 */
@Entity
@Table(name = "batch", indexes = {
    @Index(name = "batch_creation_time_idx", columnList = "creation_time")})
@NamedQuery(name = BatchRow.GET_LATEST_BATCHES_QUERY,
    query = "SELECT b FROM BatchRow b ORDER BY b.creationTime DESC ")
@NamedQuery(name = BatchRow.GET_OLD_BATCHES_QUERY, query = "SELECT b FROM BatchRow b WHERE b.creationTime <= :" +
    BatchRow.CREATION_TIME_PARAMETER)
@NamedQuery(name = BatchRow.DELETE_OLD_BATCHES_QUERY, query = "DELETE FROM BatchRow b WHERE b.creationTime <= :" +
    BatchRow.CREATION_TIME_PARAMETER)
public class BatchRow {

  public static final String GET_LATEST_BATCHES_QUERY = "getLatestBatches";
  public static final String GET_OLD_BATCHES_QUERY = "getOldBatches";
  public static final String DELETE_OLD_BATCHES_QUERY = "deleteOldBatches";
  public static final String CREATION_TIME_PARAMETER = "creationTime";

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

  @Column(name = "datasets_excluded_without_links")
  private Integer datasetsExcludedWithoutLinks;

  /**
   * Constructor for the use of JPA. Don't use from code.
   */
  protected BatchRow() {
  }

  /**
   * Constructor.
   *
   * @param creationTime the creation time of the batch
   * @param lastUpdateTimeInSolr the last update timestamp available in solr
   * @param lastUpdateTimeInMetisCore the last update timestamp available in metis core
   */
  public BatchRow(Instant creationTime, Instant lastUpdateTimeInSolr,
      Instant lastUpdateTimeInMetisCore) {
    this.creationTime = creationTime.toEpochMilli();
    this.lastUpdateTimeInSolr = lastUpdateTimeInSolr.toEpochMilli();
    this.lastUpdateTimeInMetisCore = lastUpdateTimeInMetisCore.toEpochMilli();
  }

  /**
   * Set counters.
   *
   * @param datasetsExcludedAlreadyRunning datasets excluded that are already running counter
   * @param datasetsExcludedNotIndexed datasets excluded that are not indexed counter
   * @param datasetsExcludedWithoutLinks datasets excluded that do not contain links to check
   */
  public void setCounters(int datasetsExcludedAlreadyRunning, int datasetsExcludedNotIndexed,
      int datasetsExcludedWithoutLinks) {
    this.datasetsExcludedAlreadyRunning = datasetsExcludedAlreadyRunning;
    this.datasetsExcludedNotIndexed = datasetsExcludedNotIndexed;
    this.datasetsExcludedWithoutLinks = datasetsExcludedWithoutLinks;
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

  public Integer getDatasetsExcludedWithoutLinks() {
    return datasetsExcludedWithoutLinks;
  }
}
