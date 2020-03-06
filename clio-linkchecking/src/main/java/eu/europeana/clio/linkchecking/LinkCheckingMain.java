package eu.europeana.clio.linkchecking;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.linkchecking.config.PropertiesHolder;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.CustomTruststoreAppender.TrustStoreConfigurationException;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is currently the entry point of the link checking module of Clio. It contains a main
 * method ({@link #main(String[])}) that can be used to trigger the functionality.
 */
public class LinkCheckingMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinkCheckingMain.class);

  private enum Mode{FULL_PROCESSING, LINK_CHECKING_ONLY}

  /**
   * Main method.
   *
   * @param args The input arguments.
   */
  public static void main(String[] args) {
    try {
      final String modeString = Optional.of(args).filter(list -> list.length > 0)
              .map(list -> list[0]).orElse(null);
      mainInternal(parseMode(modeString));
    } catch (ClioException | RuntimeException e) {
      LOGGER.warn("Something happened while performing link checking.", e);
      System.exit(1);
    }
  }

  private static Mode parseMode(String modeString) {

    // In case nothing is provided
    if (modeString == null) {
      LOGGER.info("No processing mode provided: defaulting to processing mode {}.",
              Mode.FULL_PROCESSING);
      return Mode.FULL_PROCESSING;
    }

    // In case the mode is not recognised.
    final Mode mode = Arrays.stream(Mode.values())
            .filter(value -> modeString.equals(value.name())).findAny().orElse(null);
    if (mode == null) {
      LOGGER.warn("Unrecognized processing mode provided: '{}'. Defaulting to processing mode {}.",
              modeString, Mode.FULL_PROCESSING);
      return Mode.FULL_PROCESSING;
    }

    // In case we have a valid mode.
    LOGGER.info("Executing with processing mode {}.", mode);
    return mode;
  }

  private static void mainInternal(Mode mode) throws ClioException {

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

    // Compute and store the sample records.
    final LinkCheckingEngine linkCheckingEngine = new LinkCheckingEngine(properties);
    if (mode != Mode.LINK_CHECKING_ONLY) {
      LOGGER.info("Creating runs for all available datasets.");
      linkCheckingEngine.createRunsForAllAvailableDatasets();
      LOGGER.info("Runs created.");
    }

    // Perform link checking on the links, updating as we go.
    LOGGER.info("Executing all pending runs.");
    linkCheckingEngine.performLinkCheckingOnAllUncheckedLinks();
    LOGGER.info("All pending runs executed.");
  }
}
