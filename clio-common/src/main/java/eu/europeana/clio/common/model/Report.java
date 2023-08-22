package eu.europeana.clio.common.model;

public class Report {
    private long reportId;
    private long batchId;
    private long creationTime;
    private String report;

    public Report(long reportId, long batchId, long creationTime, String report) {
        this.reportId = reportId;
        this.batchId = batchId;
        this.creationTime = creationTime;
        this.report = report;
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

    public String getReport() {
        return report;
    }
}
