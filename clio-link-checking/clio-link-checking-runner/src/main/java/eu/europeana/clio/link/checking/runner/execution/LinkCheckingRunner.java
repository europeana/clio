package eu.europeana.clio.link.checking.runner.execution;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.link.checking.service.config.LinkCheckingEngineConfiguration;
import eu.europeana.clio.link.checking.service.config.Mode;
import eu.europeana.clio.link.checking.service.execution.LinkCheckingEngine;
import eu.europeana.clio.reporting.service.ReportingEngine;
import eu.europeana.clio.reporting.service.config.ReportingEngineConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * This class is the main entry point of the link checking module of Clio. It contains a main method ({@link #run(String[])}) that
 * can be used to trigger the functionality.
 */
@Slf4j
public class LinkCheckingRunner implements CommandLineRunner {

  private final LinkCheckingEngineConfiguration linkCheckingEngineConfiguration;
  private final ReportingEngine reportingEngine;

  /**
   * Constructor with parameters.
   *
   * @param linkCheckingEngineConfiguration the configuration properties
   * @param reportingEngineConfiguration the reporting engine
   */
  public LinkCheckingRunner(LinkCheckingEngineConfiguration linkCheckingEngineConfiguration,
      ReportingEngineConfiguration reportingEngineConfiguration) {
    this.linkCheckingEngineConfiguration = linkCheckingEngineConfiguration;
    this.reportingEngine = new ReportingEngine(reportingEngineConfiguration);
  }

  /**
   * Main method.
   *
   * @param args The input arguments.
   */
  @Override
  public void run(String[] args) throws ClioException {
    mainInternal(linkCheckingEngineConfiguration.getLinkCheckingConfigurationProperties().getCheckingMode());
  }

  private void mainInternal(Mode mode) throws ClioException {
    final long startTime = System.nanoTime();
    log.info("Removing old data");
    final LinkCheckingEngine linkCheckingEngine = new LinkCheckingEngine(linkCheckingEngineConfiguration);
    linkCheckingEngine.removeOldData();
    log.info("Removed old data");

    if (mode != Mode.LINK_CHECKING_ONLY) {
      log.info("Creating runs for all available datasets");
      linkCheckingEngine.createRunsForAllAvailableDatasets();
      log.info("Runs created");
    }

    log.info("Executing all pending runs");
    linkCheckingEngine.performLinkCheckingOnAllUncheckedLinks();
    log.info("All pending runs executed");

    log.info("Generating report for batch");
    final String report = reportingEngine.generateReport();
    reportingEngine.storeReport(report);
    log.info("Generated report for batch");

    final long elapsedTimeInSeconds = Duration.of(System.nanoTime() - startTime, ChronoUnit.NANOS).toSeconds();
    log.info("Total time elapsed in seconds: {}", elapsedTimeInSeconds);
  }
}
