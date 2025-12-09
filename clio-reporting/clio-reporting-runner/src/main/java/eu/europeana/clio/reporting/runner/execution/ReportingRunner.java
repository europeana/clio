package eu.europeana.clio.reporting.runner.execution;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.reporting.service.ReportingEngine;
import eu.europeana.clio.reporting.service.config.ReportingEngineConfiguration;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

/**
 * This class is a command-line entry point for the reporting module of Clio. It contains a main
 * method ({@link #run(String[])}) that can be used to trigger the functionality.
 * <p>This is meant to be used as a test</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ReportingRunner implements CommandLineRunner {

    private final ReportingEngineConfiguration reportingEngineConfiguration;

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
            log.warn("Something went wrong while compiling the report.", e);
        }
    }

    private void mainInternal() throws ClioException {

        // The output file path. Prevent false positive, the user can't determine the output file.
        @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
        final Path path = Paths.get(ReportingEngine.getReportFileNameSuggestion()).toAbsolutePath();

        // Generate the report
        log.info("Saving the report to output file: {}", path);
        try (final BufferedWriter fileWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            new ReportingEngine(reportingEngineConfiguration).generateReport(fileWriter);
        } catch (IOException e) {
            throw new ClioException("Error occurred while compiling the report.", e);
        }
    }
}
