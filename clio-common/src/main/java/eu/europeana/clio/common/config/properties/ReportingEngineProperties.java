package eu.europeana.clio.common.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportingEngineProperties {

    @Value("${report.dataset.link.template}")
    private String reportDatasetLinkTemplate;

    public String getReportDatasetLinkTemplate() {
        return reportDatasetLinkTemplate;
    }
}
