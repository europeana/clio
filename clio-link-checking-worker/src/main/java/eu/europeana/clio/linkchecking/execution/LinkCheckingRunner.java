package eu.europeana.clio.linkchecking.execution;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.linkchecking.config.LinkCheckingEngineConfiguration;
import eu.europeana.clio.linkchecking.config.Mode;
import eu.europeana.clio.reporting.common.ReportingEngine;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * This class is the main entry point of the link checking module of Clio. It contains a main
 * method ({@link #run(String[])}) that can be used to trigger the functionality.
 */
public class LinkCheckingRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final LinkCheckingEngineConfiguration linkCheckingEngineConfiguration;
    private final ReportingEngine reportingEngine;

    /**
     * Constructor with parameters.
     *
     * @param linkCheckingEngineConfiguration the configuration properties
     * @param reportingEngineConfiguration the reporting engine
     */
    public LinkCheckingRunner(LinkCheckingEngineConfiguration linkCheckingEngineConfiguration, ReportingEngineConfiguration reportingEngineConfiguration) {
        this.linkCheckingEngineConfiguration = linkCheckingEngineConfiguration;
        this.reportingEngine = new ReportingEngine(reportingEngineConfiguration);
    }

    /**
     * Main method.
     *
     * @param args The input arguments.
     */
    @Override
    public void run(String... args) {
        try {
            mainInternal(linkCheckingEngineConfiguration.getLinkCheckingMode());
        } catch (ClioException | RuntimeException e) {
            LOGGER.warn("Something happened while performing link checking.", e);
        }
    }

    private void mainInternal(Mode mode) throws ClioException {
        final long startTime = System.nanoTime();
        LOGGER.info("Removing old data");
        final LinkCheckingEngine linkCheckingEngine = new LinkCheckingEngine(linkCheckingEngineConfiguration);
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

        LOGGER.info("Generating report for batch");
        final String report = reportingEngine.generateReport();
        reportingEngine.storeReport(report);
        LOGGER.info("Generated report for batch");

        final long elapsedTimeInSeconds = Duration.of(System.nanoTime() - startTime, ChronoUnit.NANOS).toSeconds();
        LOGGER.info("Total time elapsed in seconds: {}", elapsedTimeInSeconds);
    }
}
