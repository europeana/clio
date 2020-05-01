package eu.europeana.clio.reporting.rest;

import eu.europeana.clio.common.model.BatchWithCounters;
import io.swagger.annotations.ApiModelProperty;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * This class represents a batch with some extra counters. This class is meant to share in the API.
 */
public class BatchesRequestResult {

  private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
          .withZone(ZoneId.systemDefault());

  @ApiModelProperty("The moment this batch was created.")
  private final String creationTime;

  @ApiModelProperty("The last update in the Solr database at the moment this batch was created.")
  private final String lastUpdateTimeInSolr;

  @ApiModelProperty("The last indexing task in the Metis history at the moment this batch was created.")
  private final String lastUpdateTimeInMetisCore;

  @ApiModelProperty(value = "How many datasets were excluded because they were still pending from a previous batch. If absent, this number is not known.")
  private final Integer datasetsExcludedAlreadyRunning;

  @ApiModelProperty(value = "How many datasets were excluded because its data had not (yet) been indexed. If absent, this number is not known.")
  private final Integer datasetsExcludedNotIndexed;

  @ApiModelProperty(value = "How many datasets were excluded because they did not have records with links to check. If absent, this number is not known.")
  private final Integer datasetsExcludedWithoutLinks;

  @ApiModelProperty("The number of datasets/runs in this batch that have finished processing.")
  private final int datasetsProcessed;

  @ApiModelProperty("The number of datasets/runs in this batch that have not yet finished processing.")
  private final int datasetsPending;

  BatchesRequestResult(BatchWithCounters batchWithCounters) {
    this.creationTime = INSTANT_FORMATTER.format(batchWithCounters.getCreationTime());
    this.lastUpdateTimeInSolr = INSTANT_FORMATTER
            .format(batchWithCounters.getLastUpdateTimeInSolr());
    this.lastUpdateTimeInMetisCore = INSTANT_FORMATTER
            .format(batchWithCounters.getLastUpdateTimeInMetisCore());
    this.datasetsExcludedAlreadyRunning = batchWithCounters.getDatasetsExcludedAlreadyRunning();
    this.datasetsExcludedNotIndexed = batchWithCounters.getDatasetsExcludedNotIndexed();
    this.datasetsExcludedWithoutLinks = batchWithCounters.getDatasetsExcludedWithoutLinks();
    this.datasetsProcessed = batchWithCounters.getDatasetsProcessed();
    this.datasetsPending = batchWithCounters.getDatasetsPending();
  }

  public String getCreationTime() {
    return creationTime;
  }

  public String getLastUpdateTimeInSolr() {
    return lastUpdateTimeInSolr;
  }

  public String getLastUpdateTimeInMetisCore() {
    return lastUpdateTimeInMetisCore;
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

  public int getDatasetsProcessed() {
    return datasetsProcessed;
  }

  public int getDatasetsPending() {
    return datasetsPending;
  }
}
