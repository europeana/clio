package eu.europeana.clio.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class using {@link ConfigurationProperties} loading.
 */
@ConfigurationProperties(prefix = "reporting")
public class ReportingEngineProperties {

    private String datasetLinkTemplate;

    public void setDatasetLinkTemplate(String datasetLinkTemplate) {
        this.datasetLinkTemplate = datasetLinkTemplate;
    }

    public String getDatasetLinkTemplate() {
        return datasetLinkTemplate;
    }
}
