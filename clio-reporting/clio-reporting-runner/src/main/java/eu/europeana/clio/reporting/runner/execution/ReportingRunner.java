package eu.europeana.clio.reporting.runner.execution;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.reporting.service.ReportingEngine;
import eu.europeana.clio.reporting.service.config.ReportingEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class is a command-line entry point for the reporting module of Clio. It contains a main
 * method ({@link #run(String[])}) that can be used to trigger the functionality.
 * <p>This is meant to be used as a test</p>
 */
public class ReportingRunner implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportingRunner.class);

    private final ReportingEngineConfiguration reportingEngineConfiguration;

    /**
     * Constructor.
     *
     * @param reportingEngineConfiguration the reporting engine configuration
     */
    public ReportingRunner(ReportingEngineConfiguration reportingEngineConfiguration) {
        this.reportingEngineConfiguration = reportingEngineConfiguration;
    }

    /**
     * Main method.
     *
     * @param args The input arguments.
     */
    @Override
    public void run(String[] args) {
        try {
            mainInternal();
        } catch (ClioException | RuntimeException e) {
            LOGGER.warn("Something went wrong while compiling the report.", e);
        }
    }

    private void mainInternal() throws ClioException {

        // The output file path. Prevent false positive, the user can't determine the output file.
        @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
        final Path path = Paths.get(ReportingEngine.getReportFileNameSuggestion()).toAbsolutePath();

        // Generate the report
        LOGGER.info("Saving the report to output file: {}", path);
        try (final BufferedWriter fileWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            new ReportingEngine(reportingEngineConfiguration).generateReport(fileWriter);
        } catch (IOException e) {
            throw new ClioException("Error occurred while compiling the report.", e);
        }
    }
}
