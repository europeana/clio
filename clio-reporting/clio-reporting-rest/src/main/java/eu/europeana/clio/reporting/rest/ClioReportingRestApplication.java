package eu.europeana.clio.reporting.rest;

import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.reporting.core.ReportingEngine;
import eu.europeana.clio.reporting.core.config.AbstractPropertiesHolder;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@ComponentScan({"eu.europeana.clio.reporting.rest"})
public class ClioReportingRestApplication implements WebMvcConfigurer, InitializingBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClioReportingRestApplication.class);

  private final AbstractPropertiesHolder properties = new PropertiesFromSpring();

  @Override
  public void afterPropertiesSet() throws ConfigurationException {

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
