package eu.europeana.clio.linkchecking.execution;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.linkchecking.config.ConfigurationPropertiesHolder;
import eu.europeana.clio.linkchecking.config.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * This class is the main entry point of the link checking module of Clio. It contains a main
 * method ({@link #run(String[])}) that can be used to trigger the functionality.
 */
public class LinkCheckingRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkCheckingRunner.class);

    private final ConfigurationPropertiesHolder propertiesHolder;

    /**
     * Constructor with parameters.
     *
     * @param propertiesHolder the configuration properties
     */
    public LinkCheckingRunner(ConfigurationPropertiesHolder propertiesHolder) {
        this.propertiesHolder = propertiesHolder;
    }

    /**
     * Main method.
     *
     * @param args The input arguments.
     */
    @Override
    public void run(String... args) {
        try {
            mainInternal(propertiesHolder.getLinkCheckingMode());
        } catch (ClioException | RuntimeException e) {
            LOGGER.warn("Something happened while performing link checking.", e);
        }
    }

    private void mainInternal(Mode mode) throws ClioException {
        final long startTime = System.nanoTime();
        LOGGER.info("Removing old data");
        final LinkCheckingEngine linkCheckingEngine = new LinkCheckingEngine(propertiesHolder);
        linkCheckingEngine.removeOldData();
        LOGGER.info("Removed old data");

        if (mode != Mode.LINK_CHECKING_ONLY) {
            LOGGER.info("Creating runs for all available datasets");
            linkCheckingEngine.createRunsForAllAvailableDatasets();
            LOGGER.info("Runs created");
        }

        LOGGER.info("Executing all pending runs");
        linkCheckingEngine.performLinkCheckingOnAllUncheckedLinks();
        LOGGER.info("All pending runs executed");

        final long elapsedTimeInSeconds = Duration.of(System.nanoTime() - startTime, ChronoUnit.NANOS).toSeconds();
        LOGGER.info("Total time elapsed in seconds: {}", elapsedTimeInSeconds);
    }
}
