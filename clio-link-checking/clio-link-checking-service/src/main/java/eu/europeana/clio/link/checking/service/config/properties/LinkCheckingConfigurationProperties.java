package eu.europeana.clio.link.checking.service.config.properties;

import eu.europeana.clio.link.checking.service.config.Mode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class using {@link ConfigurationProperties} loading.
 */
@ConfigurationProperties(prefix = "link-checking")
public class LinkCheckingConfigurationProperties {

    private static final Mode DEFAULT_MODE = Mode.FULL_PROCESSING;
    private static final int DEFAULT_RETENTION_MONTHS = 6;
    private Mode checkingMode = DEFAULT_MODE;
    private int retentionMonths = DEFAULT_RETENTION_MONTHS;
    private int sampleRecordsPerDataset;
    private int runCreateThreads;
    private int runExecuteThreads;
    private int minTimeBetweenSameServerChecks;
    private int connectTimeout;
    private int responseTimeout;
    private int downloadTimeout;

    public void setCheckingMode(Mode checkingMode) {
        this.checkingMode = checkingMode;
    }

    public void setRetentionMonths(int retentionMonths) {
        this.retentionMonths = retentionMonths;
    }

    public void setSampleRecordsPerDataset(int sampleRecordsPerDataset) {
        this.sampleRecordsPerDataset = sampleRecordsPerDataset;
    }

    public void setRunCreateThreads(int runCreateThreads) {
        this.runCreateThreads = runCreateThreads;
    }

    public void setRunExecuteThreads(int runExecuteThreads) {
        this.runExecuteThreads = runExecuteThreads;
    }

    public void setMinTimeBetweenSameServerChecks(int minTimeBetweenSameServerChecks) {
        this.minTimeBetweenSameServerChecks = minTimeBetweenSameServerChecks;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setResponseTimeout(int responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public void setDownloadTimeout(int downloadTimeout) {
        this.downloadTimeout = downloadTimeout;
    }

    public Mode getCheckingMode() {
        return checkingMode;
    }

    public int getRetentionMonths() {
        return retentionMonths;
    }

    public int getSampleRecordsPerDataset() {
        return sampleRecordsPerDataset;
    }

    public int getRunCreateThreads() {
        return runCreateThreads;
    }

    public int getRunExecuteThreads() {
        return runExecuteThreads;
    }

    public int getMinTimeBetweenSameServerChecks() {
        return minTimeBetweenSameServerChecks;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getResponseTimeout() {
        return responseTimeout;
    }

    public int getDownloadTimeout() {
        return downloadTimeout;
    }
}
