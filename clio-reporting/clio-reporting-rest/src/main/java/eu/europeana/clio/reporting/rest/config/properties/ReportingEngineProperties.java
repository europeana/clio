package eu.europeana.clio.reporting.rest.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportingEngineProperties {
    // Reporting
    @Value("${report.dataset.link.template}")
    private String reportDatasetLinkTemplate;

    public String getReportDatasetLinkTemplate() {
        return reportDatasetLinkTemplate;
    }
}
