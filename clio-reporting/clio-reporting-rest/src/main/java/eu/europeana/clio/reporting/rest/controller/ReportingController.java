package eu.europeana.clio.reporting.rest.controller;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.reporting.common.ReportingEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The controller (web endpoint) that provides functionality related to the link checking report.
 */
@Controller
@Tags(@Tag(name = ReportingController.CONTROLLER_TAG_NAME,
        description = "Controller providing access to link checking results and history."))
public class ReportingController {

  public static final String CONTROLLER_TAG_NAME = "ReportingController";

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
  @Operation(summary = "Get full report with the the latest link checking results.", description = "The links in the report may be part of multiple batches.")
  public HttpEntity<byte[]> getReport() throws ClioException {

    // Create the report.
    byte[] report;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final BufferedWriter outputWriter = new BufferedWriter(
                    new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
      this.reportingEngine.generateReport(outputWriter);
      report = outputStream.toByteArray();
    } catch (IOException e) {
      throw new ClioException("Problem while retrieving report.", e);
    } catch (ClioException | RuntimeException e) {
      throw e;
    }

    // Return the report.
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(ContentDisposition.builder("inline")
            .filename(ReportingEngine.getReportFileNameSuggestion()).build());
    headers.setContentLength(report.length);
    return new HttpEntity<>(report, headers);
  }

  /**
   * Get a historic overview of the most recent link checking batches.
   *
   * @param maxResults the maximum number of results to return
   * @return the most recent batches
   * @throws ClioException if an error occurred while retrieving the most recent batches
   */
  @GetMapping(value = "batches", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Operation(summary =
          "Get a historic overview of the most recent link checking batches.", description = "The batches are returned in reverse chronological order.")
  public ResponseEntity<List<BatchesRequestResult>> getBatches(
          @RequestParam(value = "maxResults", required = false, defaultValue = "5")
          @Parameter(description = "The maximum number of batches returned, must be a positive number.", example = "1")
                  int maxResults)
          throws ClioException {
    if (maxResults < 1) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    final List<BatchesRequestResult> result = this.reportingEngine.getLatestBatches(maxResults)
            .stream().map(BatchesRequestResult::new).collect(Collectors.toList());
    return new ResponseEntity<>(result, HttpStatus.OK);
  }
}
