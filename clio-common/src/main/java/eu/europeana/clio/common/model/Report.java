package eu.europeana.clio.common.model;

/**
 * Class representing a report relevant to a batch.
 */
public class Report {
    private final long reportId;
    private final long batchId;
    private final long creationTime;
    private final String reportString;

    /**
     * Constructor.
     *
     * @param reportId the report identifier
     * @param batchId the batch identifier
     * @param creationTime the creation time of the report
     * @param reportString the report file in string format
     */
    public Report(long reportId, long batchId, long creationTime, String reportString) {
        this.reportId = reportId;
        this.batchId = batchId;
        this.creationTime = creationTime;
        this.reportString = reportString;
    }

    public long getReportId() {
        return reportId;
    }

    public long getBatchId() {
        return batchId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getReportString() {
        return reportString;
    }
}
