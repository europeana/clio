package eu.europeana.clio.common.model;

import java.time.Instant;

public class Batch {

  private final long batchId;
  private final Instant creationTime;
  private final Instant lastUpdateTimeInSolr;
  private final Instant lastUpdateTimeInMetsiCore;
  private Integer datasetsExcludedAlreadyRunning;
  private Integer datasetsExcludedNotIndexed;

  public Batch(long batchId, Instant creationTime, Instant lastUpdateTimeInSolr,
          Instant lastUpdateTimeInMetsiCore, Integer datasetsExcludedAlreadyRunning,
          Integer datasetsExcludedNotIndexed) {
    this.batchId = batchId;
    this.creationTime = creationTime;
    this.lastUpdateTimeInSolr = lastUpdateTimeInSolr;
    this.lastUpdateTimeInMetsiCore = lastUpdateTimeInMetsiCore;
    this.datasetsExcludedAlreadyRunning = datasetsExcludedAlreadyRunning;
    this.datasetsExcludedNotIndexed = datasetsExcludedNotIndexed;
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
}
