package eu.europeana.clio.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Check Run Record that represents a single filtering result
 */
@JsonSerialize
public record CheckRunRecord(Long id,
                             @Schema(pattern = "yyyy-MM-dd", example = "2026-01-01")
                             @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
                             Date date,
                             String datasetId,
                             String datasetName,
                             Long datasetSize,
                             @Schema(pattern = "yyyy-MM-dd", example = "2026-01-01")
                             @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
                             Date datasetLastIndex,
                             String provider,
                             String dataProvider,
                             int percentLinksInOperation) {

  /**
   * Instantiates a new Check Run record.
   *
   * @param id the id
   * @param date the date
   * @param datasetId the dataset id
   * @param datasetName the dataset name
   * @param datasetSize the dataset size
   * @param datasetLastIndex the dataset date last index
   * @param provider the provider
   * @param dataProvider the data provider
   * @param percentLinksInOperation the percent links in operation
   */
  public CheckRunRecord(Long id, Long date,
      String datasetId, String datasetName, Long datasetSize, Long datasetLastIndex,
      String provider, String dataProvider, int percentLinksInOperation) {
    this(id, new Date(date), datasetId, datasetName, datasetSize, new Date(datasetLastIndex), provider, dataProvider,
        percentLinksInOperation);
  }
}
