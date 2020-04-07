package eu.europeana.clio.common.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * This class represents a batch with some extra counters. This class is meant for serializing.
 */
public class BatchWithCounters {

  private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
          .withZone(ZoneId.systemDefault());

  private final long batchId;
  private final String creationTime;
  private final String lastUpdateTimeInSolr;
  private final String lastUpdateTimeInMetsiCore;
  private final Integer datasetsExcludedAlreadyRunning;
  private final Integer datasetsExcludedNotIndexed;
  private final int datasetsProcessed;
  private final int datasetsPending;

  public BatchWithCounters(long batchId, Instant creationTime, Instant lastUpdateTimeInSolr,
          Instant lastUpdateTimeInMetsiCore, Integer datasetsExcludedAlreadyRunning,
          Integer datasetsExcludedNotIndexed, int datasetsProcessed, int datasetsPending) {
    this.batchId = batchId;
    this.creationTime = INSTANT_FORMATTER.format(creationTime);
    this.lastUpdateTimeInSolr = INSTANT_FORMATTER.format(lastUpdateTimeInSolr);
    this.lastUpdateTimeInMetsiCore = INSTANT_FORMATTER.format(lastUpdateTimeInMetsiCore);
    this.datasetsExcludedAlreadyRunning = datasetsExcludedAlreadyRunning;
    this.datasetsExcludedNotIndexed = datasetsExcludedNotIndexed;
    this.datasetsProcessed = datasetsProcessed;
    this.datasetsPending = datasetsPending;
  }

  public long getBatchId() {
    return batchId;
  }

  public String getCreationTime() {
    return creationTime;
  }

  public String getLastUpdateTimeInSolr() {
    return lastUpdateTimeInSolr;
  }

  public String getLastUpdateTimeInMetsiCore() {
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
