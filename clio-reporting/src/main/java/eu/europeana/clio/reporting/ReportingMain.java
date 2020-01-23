package eu.europeana.clio.reporting;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.reporting.config.PropertiesHolder;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is currently the entry point of the reporting module of Clio. It contains a main
 * method ({@link #main(String[])}) that can be used to trigger the functionality.
 */
public class ReportingMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportingMain.class);

  private static final String OUTPUT_FILE = "/home/jochen/Desktop/clio/report.csv";

  /**
   * Main method.
   *
   * @param args The input arguments.
   */
  public static void main(String[] args) {
    try {
      mainInternal();
    } catch (ClioException | RuntimeException e) {
      LOGGER.warn(e.getMessage(), e);
    }
  }

  private static void mainInternal() throws ClioException {

    // Read the properties
    final PropertiesHolder properties = new PropertiesHolder();

    // Set the truststore.
    LOGGER.info("Append default truststore with custom truststore");
    if (StringUtils.isNotEmpty(properties.getTruststorePath()) && StringUtils
            .isNotEmpty(properties.getTruststorePassword())) {
      try {
        CustomTruststoreAppender.appendCustomTrustoreToDefault(properties.getTruststorePath(),
                properties.getTruststorePassword());
      } catch (TrustStoreConfigurationException e) {
        throw new ConfigurationException(e.getMessage(), e);
      }
    }

    // Generate the report
    LOGGER.info("Saving the report to output file: {}", OUTPUT_FILE);
    new ReportingEngine(properties).generateReport(OUTPUT_FILE);
  }
}
