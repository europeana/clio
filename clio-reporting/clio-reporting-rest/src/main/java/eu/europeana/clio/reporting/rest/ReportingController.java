package eu.europeana.clio.reporting.rest;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.reporting.core.ReportingEngine;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The controller (web endpoint) that provides functionality related to the link checking report.
 */
@Controller
@Tags(@Tag(name = ReportingController.CONTROLLER_TAG_NAME,
        description = "Controller providing access to link checking results and history."))
@Api(tags = ReportingController.CONTROLLER_TAG_NAME)
public class ReportingController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportingController.class);

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
  @ApiOperation(value = "Get full report with the the latest link checking results.",
          notes = "The links in the report may be part of multiple batches.")
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
  @ApiOperation(value = "Get a historic overview of the most recent link checking batches.",
          notes = "The batches are returned in reverse chronological order.")
  public ResponseEntity<List<BatchesRequestResult>> getBatches(
          @RequestParam(value = "maxResults", required = false, defaultValue = "5")
          @ApiParam(value = "The maximum number of batches returned, must be a positive number.", example = "1")
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
