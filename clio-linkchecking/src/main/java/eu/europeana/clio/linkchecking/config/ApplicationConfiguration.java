package eu.europeana.clio.linkchecking.config;

import eu.europeana.clio.linkchecking.execution.LinkCheckingRunner;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
public class ApplicationConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    private final ConfigurationPropertiesHolder propertiesHolder;


    /**
     * Autowired constructor for Spring Configuration class.
     *
     * @param propertiesHolder the object that holds all boot configuration values
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException if the configuration of the truststore failed
     */
    public ApplicationConfiguration(ConfigurationPropertiesHolder propertiesHolder) throws CustomTruststoreAppender.TrustStoreConfigurationException {
        ApplicationConfiguration.initializeApplication(propertiesHolder);
        this.propertiesHolder = propertiesHolder;
    }

    /**
     * This method performs the initializing tasks for the application.
     *
     * @param propertiesHolder The properties.
     * @throws CustomTruststoreAppender.TrustStoreConfigurationException In case a problem occurred with the truststore.
     */
    static void initializeApplication(ConfigurationPropertiesHolder propertiesHolder)
            throws CustomTruststoreAppender.TrustStoreConfigurationException {

        // Load the trust store file.
        if (StringUtils.isNotEmpty(propertiesHolder.getTruststorePath()) && StringUtils
                .isNotEmpty(propertiesHolder.getTruststorePassword())) {
            CustomTruststoreAppender
                    .appendCustomTrustoreToDefault(propertiesHolder.getTruststorePath(),
                            propertiesHolder.getTruststorePassword());
            LOGGER.info("Custom truststore appended to default truststore");
        }
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return new LinkCheckingRunner(propertiesHolder);
    }
}
