package eu.europeana.clio.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Dataset summary record that represents a single filtering result
 */
@JsonSerialize
public record DatasetSummary(String datasetId,
                             String datasetName,
                             Long datasetSize,
                             @Schema(pattern = "yyyy-MM-dd", example = "2026-01-01")
                             @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
                             LocalDate datasetLastIndex,
                             String provider,
                             String dataProvider,
                             int percentLinksInOperation) {

  /**
   * Instantiates a new run summary record.
   *
   * @param datasetId the dataset id
   * @param datasetName the dataset name
   * @param datasetSize the dataset size
   * @param datasetLastIndex the dataset date last index
   * @param provider the provider
   * @param dataProvider the data provider
   * @param percentLinksInOperation the percent links in operation
   */
  public DatasetSummary(
      String datasetId, String datasetName, Long datasetSize, Long datasetLastIndex,
      String provider, String dataProvider, int percentLinksInOperation) {
    this(datasetId, datasetName, datasetSize, Instant.ofEpochMilli(datasetLastIndex).atZone(ZoneOffset.UTC).toLocalDate(),
        provider, dataProvider, percentLinksInOperation);
  }
}
