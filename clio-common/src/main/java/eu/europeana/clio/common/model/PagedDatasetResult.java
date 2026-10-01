package eu.europeana.clio.common.model;

import java.util.List;

/**
 * The type Paged dataset result.
 */
public record PagedDatasetResult(List<DatasetSummary> datasetSummaries, Pagination pagination) {

}
