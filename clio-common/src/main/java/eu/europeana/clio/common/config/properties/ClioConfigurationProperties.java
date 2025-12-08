package eu.europeana.clio.common.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Class using {@link ConfigurationProperties} loading.
 */
@ConfigurationProperties(prefix = "clio")
public record ClioConfigurationProperties(String datasetReportLinkTemplate, List<String> allowedCorsHosts) {

}
