package eu.europeana.clio.reporting.rest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.reporting.core.ReportingEngine;
import eu.europeana.clio.reporting.core.config.AbstractPropertiesHolder;
import eu.europeana.metis.utils.CustomTruststoreAppender;

@Configuration
@ComponentScan(basePackages = {"eu.europeana.clio.reporting.rest"})
@EnableWebMvc
public class ClioReportingRestApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClioReportingRestApplication.class);

  private final AbstractPropertiesHolder properties;

  @Autowired
  public ClioReportingRestApplication(AbstractPropertiesHolder propertiesHolder)
      throws ConfigurationException {

    // Set the properties.
    this.properties = propertiesHolder;

    // Set the truststore.
    LOGGER.info("Append default truststore with custom truststore");
    if (StringUtils.isNotEmpty(properties.getTruststorePath())
        && StringUtils.isNotEmpty(properties.getTruststorePassword())) {
      try {
        CustomTruststoreAppender.appendCustomTrustoreToDefault(properties.getTruststorePath(),
            properties.getTruststorePassword());
      } catch (TrustStoreConfigurationException e) {
        throw new ConfigurationException(e.getMessage(), e);
      }
    }
  }

  @Bean
  ReportingEngine getReportingEngine() {
    return new ReportingEngine(properties);
  }
}
