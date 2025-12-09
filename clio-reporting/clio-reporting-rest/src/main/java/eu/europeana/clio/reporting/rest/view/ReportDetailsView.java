package eu.europeana.clio.reporting.rest.view;

import java.time.Instant;

/**
 * Represents the report details.
 */
public record ReportDetailsView(long reportId, long batchId, Instant creationTime, String url) {

}
