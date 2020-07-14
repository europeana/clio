package eu.europeana.clio.linkchecking.config;

import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.CustomTruststoreAppender.TrustStoreConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utilities class initializes the trust store and
 */
public final class TruststoreInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(TruststoreInitializer.class);

  private TruststoreInitializer() {
    // This class should not be instantiated.
  }

  /**
   * Initialize the truststore.
   *
   * @param properties The properties.
   * @throws ConfigurationException In case something went wrong.
   */
  public static void initializeTruststore(PropertiesHolder properties)
          throws ConfigurationException {
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
  }
}
