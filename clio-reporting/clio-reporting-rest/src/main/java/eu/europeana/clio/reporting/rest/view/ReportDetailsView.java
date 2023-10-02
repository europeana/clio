package eu.europeana.clio.reporting.rest.view;

import java.time.Instant;

/**
 * Represents the report details.
 */
public class ReportDetailsView {
    private final long reportId;
    private final long batchId;
    private final Instant creationTime;
    private final String url;

    /**
     * Constructor.
     *
     * @param reportId the report identifier
     * @param batchId the batch identifier
     * @param creationTime the creation time of the report
     * @param url the report file in string format
     */
    public ReportDetailsView(long reportId, long batchId, Instant creationTime, String url) {
        this.reportId = reportId;
        this.batchId = batchId;
        this.creationTime = creationTime;
        this.url = url;
    }

    public long getReportId() {
        return reportId;
    }

    public long getBatchId() {
        return batchId;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public String getUrl() {
        return url;
    }
}
