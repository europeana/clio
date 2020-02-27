package eu.europeana.clio.reporting.rest;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.reporting.core.ReportingEngine;
import eu.europeana.clio.reporting.core.config.AbstractPropertiesHolder;
import eu.europeana.clio.reporting.core.config.PropertiesFromFile;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.CustomTruststoreAppender.TrustStoreConfigurationException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a command-line entry point for the reporting module of Clio. It contains a main
 * method ({@link #main(String[])}) that can be used to trigger the functionality.
 */
public class ReportingMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportingMain.class);

  private static final String CONFIGURATION_FILE = "application.properties";

  /**
   * Main method.
   *
   * @param args The input arguments.
   */
  public static void main(String[] args) {
    try {
      mainInternal();
    } catch (ClioException | RuntimeException e) {
      LOGGER.warn("Something went wrong while compiling the report.", e);
    }
  }

  private static void mainInternal() throws ClioException {

    // Read the properties
    final AbstractPropertiesHolder properties =
        new PropertiesFromFile(() -> Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(CONFIGURATION_FILE));

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

    // Generate the report
    final Path path = Paths.get(ReportingEngine.getReportFileNameSuggestion()).toAbsolutePath();
    LOGGER.info("Saving the report to output file: {}", path);
    try (final BufferedWriter fileWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      new ReportingEngine(properties).generateReport(fileWriter);
    } catch (IOException e) {
      throw new ClioException("Error occurred while compiling the report.", e);
    }
  }
}
