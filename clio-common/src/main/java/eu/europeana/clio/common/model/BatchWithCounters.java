package eu.europeana.clio.common.model;

import java.time.Instant;

/**
 * This class represents a batch with some extra counters.
 */
public class BatchWithCounters {

  private final long batchId;
  private final Instant creationTime;
  private final Instant lastUpdateTimeInSolr;
  private final Instant lastUpdateTimeInMetsiCore;
  private final Integer datasetsExcludedAlreadyRunning;
  private final Integer datasetsExcludedNotIndexed;
  private final int datasetsProcessed;
  private final int datasetsPending;

  public BatchWithCounters(long batchId, Instant creationTime, Instant lastUpdateTimeInSolr,
          Instant lastUpdateTimeInMetsiCore, Integer datasetsExcludedAlreadyRunning,
          Integer datasetsExcludedNotIndexed, int datasetsProcessed, int datasetsPending) {
    this.batchId = batchId;
    this.creationTime = creationTime;
    this.lastUpdateTimeInSolr = lastUpdateTimeInSolr;
    this.lastUpdateTimeInMetsiCore = lastUpdateTimeInMetsiCore;
    this.datasetsExcludedAlreadyRunning = datasetsExcludedAlreadyRunning;
    this.datasetsExcludedNotIndexed = datasetsExcludedNotIndexed;
    this.datasetsProcessed = datasetsProcessed;
    this.datasetsPending = datasetsPending;
  }

  public long getBatchId() {
    return batchId;
  }

  public Instant getCreationTime() {
    return creationTime;
  }

  public Instant getLastUpdateTimeInSolr() {
    return lastUpdateTimeInSolr;
  }

  public Instant getLastUpdateTimeInMetsiCore() {
    return lastUpdateTimeInMetsiCore;
  }

  public Integer getDatasetsExcludedAlreadyRunning() {
    return datasetsExcludedAlreadyRunning;
  }

  public Integer getDatasetsExcludedNotIndexed() {
    return datasetsExcludedNotIndexed;
  }

  public int getDatasetsProcessed() {
    return datasetsProcessed;
  }

  public int getDatasetsPending() {
    return datasetsPending;
  }
}
