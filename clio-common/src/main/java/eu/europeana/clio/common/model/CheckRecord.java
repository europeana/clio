package eu.europeana.clio.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import tools.jackson.databind.annotation.JsonSerialize;


/**
 * Record that represents a single filtering result
 */
@JsonSerialize
public record CheckRecord(long id,
                          @Schema(pattern = "yyyy-MM-dd", example = "2026-01-01")
                          @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
                          Instant date,
                          String datasetId,
                          String datasetName,
                          Long datasetSize,
                          @Schema(pattern = "yyyy-MM-dd", example = "2026-01-01")
                          @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
                          Instant datasetLastIndex,
                          String provider,
                          String dataProvider,
                          int percentLinksInOperation) {

}
