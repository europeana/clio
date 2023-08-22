package eu.europeana.clio.linkchecking.config.properties;

import eu.europeana.clio.linkchecking.config.Mode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LinkCheckingProperties {

    // Link checking
    @Value("#{T(eu.europeana.clio.linkchecking.config.Mode).getMode('${link.checking.mode}')}")
    private Mode linkCheckingMode;
    @Value("${link.checking.retention.months:6}")
    private int linkCheckingRetentionMonths;
    @Value("${link.checking.sample.records.per.dataset}")
    private int linkCheckingSampleRecordsPerDataset;
    @Value("${link.checking.run.create.threads}")
    private int linkCheckingRunCreateThreads;
    @Value("${link.checking.run.execute.threads}")
    private int linkCheckingRunExecuteThreads;
    @Value("${link.checking.min.time.between.same.server.checks}")
    private int linkCheckingMinTimeBetweenSameServerChecks;
    @Value("${link.checking.connect.timeout}")
    private int linkCheckingConnectTimeout;
    @Value("${link.checking.response.timeout}")
    private int linkCheckingResponseTimeout;
    @Value("${link.checking.download.timeout}")
    private int linkCheckingDownloadTimeout;

    public Mode getLinkCheckingMode() {
        return linkCheckingMode;
    }

    public int getLinkCheckingRetentionMonths() {
        return linkCheckingRetentionMonths;
    }

    public int getLinkCheckingSampleRecordsPerDataset() {
        return linkCheckingSampleRecordsPerDataset;
    }

    public int getLinkCheckingRunCreateThreads() {
        return linkCheckingRunCreateThreads;
    }

    public int getLinkCheckingRunExecuteThreads() {
        return linkCheckingRunExecuteThreads;
    }

    public int getLinkCheckingMinTimeBetweenSameServerChecks() {
        return linkCheckingMinTimeBetweenSameServerChecks;
    }

    public int getLinkCheckingConnectTimeout() {
        return linkCheckingConnectTimeout;
    }

    public int getLinkCheckingResponseTimeout() {
        return linkCheckingResponseTimeout;
    }

    public int getLinkCheckingDownloadTimeout() {
        return linkCheckingDownloadTimeout;
    }
}
