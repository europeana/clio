package eu.europeana.clio.reporting.rest;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.BatchWithCounters;
import eu.europeana.clio.reporting.core.ReportingEngine;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The controller (web endpoint) that provides functionality related to the link checking report.
 */
@Controller
public class ReportingController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportingController.class);

  private final ReportingEngine reportingEngine;

  /**
   * Constructor.
   *
   * @param reportingEngine The engine that can compile link checking reports.
   */
  @Autowired
  public ReportingController(ReportingEngine reportingEngine) {
    this.reportingEngine = reportingEngine;
  }

  /**
   * Computes and returns the latest version of the link checking report.
   *
   * TODO JV In the future we can cache the result so that we don't have to compute it every time.
   * We can even invalidate it if we have a new execution (or we can check the most recent run
   * starting time in the DB).
   *
   * @return The link checking report as a byte array (UTF-8 encoded).
   * @throws ClioException In case there was a problem getting the report.
   */
  @GetMapping(value = "report", produces = "text/csv")
  @ResponseBody
  public HttpEntity<byte[]> getReport() throws ClioException {

    // Create the report.
    byte[] report;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final BufferedWriter outputWriter = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
      this.reportingEngine.generateReport(outputWriter);
      report = outputStream.toByteArray();
    } catch (IOException e) {
      LOGGER.warn("IO Problem while retrieving report.", e);
      throw new ClioException("Problem while retrieving report.", e);
    } catch (ClioException | RuntimeException e) {
      LOGGER.warn("Unexpected Problem while retrieving report.", e);
      throw e;
    }

    // Return the report.
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(ContentDisposition.builder("inline")
            .filename(ReportingEngine.getReportFileNameSuggestion()).build());
    headers.setContentLength(report.length);
    return new HttpEntity<>(report, headers);
  }

  @GetMapping(value = "batches", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<BatchWithCounters> getBatches(
          @RequestParam(value = "maxResults", required = false, defaultValue = "5") int maxResults)
          throws ClioException {
    return this.reportingEngine.getLatestBatches(maxResults);
  }
}
