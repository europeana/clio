package eu.europeana.clio.reporting.rest.controller;

import eu.europeana.clio.common.model.BatchWithCounters;
import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.annotations.ApiModelProperty;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * This class represents a batch with some extra counters. This class is meant to share in the API.
 */
public class BatchesRequestResult {

  private static final DateTimeFormatter INSTANT_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
          .withZone(ZoneId.systemDefault());

  @Schema(title = "The moment this batch was created.", type = "string")
  private final String creationTime;

  @Schema(title = "The last update in the Solr database at the moment this batch was created.", type = "string")
  private final String lastUpdateTimeInSolr;

  @Schema(title = "The last indexing task in the Metis history at the moment this batch was created.", type = "string")
  private final String lastUpdateTimeInMetisCore;

  @Schema(title = "How many datasets were excluded because they were still pending from a previous batch. If absent, this number is not known.", type = "integer")
  private final Integer datasetsExcludedAlreadyRunning;

  @Schema(title = "How many datasets were excluded because its data had not (yet) been indexed. If absent, this number is not known.", type = "integer")
  private final Integer datasetsExcludedNotIndexed;

  @Schema(title = "How many datasets were excluded because they did not have records with links to check. If absent, this number is not known.", type = "integer")
  private final Integer datasetsExcludedWithoutLinks;

  @Schema(title = "The number of datasets/runs in this batch that have finished processing.", type = "integer")
  private final int datasetsProcessed;

  @Schema(title = "The number of datasets/runs in this batch that have not yet finished processing.", type = "integer")
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
