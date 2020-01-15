package eu.europeana.clio.linkchecking;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.clio.linkchecking.config.PropertiesHolder;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.net.ssl.TrustStoreConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is currently the entry point of the link checking module of Clio. It contains a main
 * method ({@link #main(String[])}) that can be used to trigger the functionality.
 */
public class LinkCheckingMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinkCheckingMain.class);

  private static final String DATASET_ID = "6";

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

    // Compute and store the sample records.
    final LinkCheckingEngine linkCheckingEngine = new LinkCheckingEngine(properties);
    linkCheckingEngine.createRunWithUncheckedLinksForDataset(DATASET_ID);

    // Perform link checking on the links, updating as we go.
    linkCheckingEngine.performLinkCheckingOnAllUncheckedLinks();
  }
}
