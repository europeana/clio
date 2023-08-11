package eu.europeana.clio.reporting.rest.config;

import eu.europeana.clio.reporting.core.config.ConfigurationPropertiesHolder;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.CustomTruststoreAppender.TrustStoreConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for performing initializing tasks for the application.
 */
final class ApplicationInitUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationInitUtils.class);

    private ApplicationInitUtils() {
    }

    /**
     * This method performs the initializing tasks for the application.
     *
     * @param propertiesHolder The properties.
     * @throws TrustStoreConfigurationException In case a problem occurred with the truststore.
     */
    static void initializeApplication(ConfigurationPropertiesHolder propertiesHolder)
            throws TrustStoreConfigurationException {

        // Load the trust store file.
        if (StringUtils.isNotEmpty(propertiesHolder.getTruststorePath()) && StringUtils
                .isNotEmpty(propertiesHolder.getTruststorePassword())) {
            CustomTruststoreAppender
                    .appendCustomTrustoreToDefault(propertiesHolder.getTruststorePath(),
                            propertiesHolder.getTruststorePassword());
            LOGGER.info("Custom truststore appended to default truststore");
        }
    }
}
