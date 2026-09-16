package eu.europeana.clio.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * The type Dataset check summary.
 */
public record DatasetCheckSummary(Long runId,
                                  @Schema(pattern = "yyyy-MM-dd", example = "2026-01-01")
                                  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
                                  LocalDate startingTime,
                                  int percentLinksInOperation) {

  /**
   * Instantiates a new Dataset check summary.
   *
   * @param runId the runId
   * @param startingTime the starting time
   * @param percentLinksInOperation the percent links in operation
   */
  public DatasetCheckSummary(Long runId, Long startingTime, int percentLinksInOperation) {
    this(runId, Instant.ofEpochMilli(startingTime).atZone(ZoneOffset.UTC).toLocalDate(), percentLinksInOperation);
  }
}
