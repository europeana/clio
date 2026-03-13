package eu.europeana.clio.common.model;

import java.time.Instant;
import tools.jackson.databind.annotation.JsonSerialize;


/**
 * Record that represents a single filtering result
 */
@JsonSerialize
public record CheckRecord(long id,
                          Instant date,
                          String datasetId,
                          String datasetName,
                          Long datasetSize,
                          Instant datasetLastIndex,
                          String provider,
                          String dataProvider,
                          int percentLinksInOperation) {

}
