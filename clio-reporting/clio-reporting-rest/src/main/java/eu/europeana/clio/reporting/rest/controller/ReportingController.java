package eu.europeana.clio.reporting.rest.controller;

import static eu.europeana.clio.common.model.FieldFilters.sanitizeFieldFilters;
import static eu.europeana.clio.reporting.rest.controller.ControllerUtils.getHttpEntity;


import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ReportNotFoundException;
import eu.europeana.clio.common.model.RunSummary;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.Report;
import eu.europeana.clio.reporting.rest.api.request.FilterRequest;
import eu.europeana.clio.reporting.rest.api.response.FilterResponse;
import eu.europeana.clio.reporting.rest.controller.advice.ErrorResponse;
import eu.europeana.clio.reporting.rest.view.ReportDetailsView;
import eu.europeana.clio.reporting.service.ReportingEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The controller (web endpoint) that provides functionality related to the link checking report.
 */
@RestController
@Tags(@Tag(name = ReportingController.CONTROLLER_TAG_NAME,
    description = "Controller providing access to link checking results and history."))
public class ReportingController {

  public static final String CONTROLLER_TAG_NAME = "ReportingController";
  public static final String AVAILABLE_REPORTS_ENDPOINT_PATH = "/available-reports";
  public static final String REPORT_BY_BATCH_ID_ENDPOINT_PATH = "/report-by-batch-id";
  public static final String LATEST_REPORT_ENDPOINT_PATH = "/latest-report";
  public static final String BATCHES_ENDPOINT_PATH = "/batches";
  public static final String BATCH_ID_ENDPOINT_PARAMETER = "batchId";
  public static final String REPORT_ID_ENDPOINT_PARAMETER = "reportId";
  public static final String REPORTS_ENDPOINT_PATH = "/reports";
  public static final String RUNS_SUMMARY_ENDPOINT_PATH = "/runs/summary";
  public static final String RUNS_LINKS_EXPORT_ENDPOINT_PATH = "/runs/links/export";

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
   * Get all available report details.
   *
   * @return the report details
   * @throws ClioException if an error occurred
   */
  @GetMapping(value = AVAILABLE_REPORTS_ENDPOINT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get all available report details")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = @Content(schema = @Schema(implementation = ReportDetailsView.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "500", description = "Persistence error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  public ResponseEntity<List<ReportDetailsView>> availableReports() throws ClioException {
    final List<Report> allReports = reportingEngine.getAllReportDetails();
    final List<ReportDetailsView> reportDetailsViews = allReports.stream().map(
        report -> {
          Instant instant = Instant.ofEpochMilli(report.getCreationTime());
          final String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                                                        .path(REPORT_BY_BATCH_ID_ENDPOINT_PATH)
                                                        .queryParam(BATCH_ID_ENDPOINT_PARAMETER, report.getBatchId())
                                                        .toUriString();
          return new ReportDetailsView(report.getReportId(), report.getBatchId(), instant, url);
        }).toList();

    return new ResponseEntity<>(reportDetailsViews, HttpStatus.OK);
  }

  /**
   * Get a report by providing its creation time
   *
   * @param batchId the batch id
   * @return the report
   * @throws ClioException if an error occurred
   */
  @GetMapping(value = REPORT_BY_BATCH_ID_ENDPOINT_PATH, produces = {"text/csv", MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get a report by creation timestamp")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(mediaType = "text/csv"), @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
      @ApiResponse(responseCode = "404", description = "Report not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "500", description = "Persistence error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  public HttpEntity<byte[]> getReportByBatchId(@RequestParam(value = BATCH_ID_ENDPOINT_PARAMETER) Long batchId)
      throws ClioException {
    final Report report = reportingEngine.getReportByBatchId(batchId);
    if (report == null) {
      throw new ReportNotFoundException(String.format("Report with batch id %s not found.", batchId));
    }
    final byte[] reportContent = report.getReportString().getBytes(StandardCharsets.UTF_8);
    return getHttpEntity(reportContent);
  }

  /**
   * Get a report by providing its report id
   *
   * @param reportId the report id
   * @return the report
   * @throws ClioException if an error occurred
   */
  @GetMapping(value = REPORTS_ENDPOINT_PATH, produces = {"text/csv", MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get a report by report id")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(mediaType = "text/csv"), @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
      @ApiResponse(responseCode = "404", description = "Report not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "500", description = "Persistence error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  public HttpEntity<byte[]> getReportById(@RequestParam(value = REPORT_ID_ENDPOINT_PARAMETER) Long reportId)
      throws ClioException {
    final Report report = reportingEngine.getReportByReportId(reportId);
    if (report == null) {
      throw new ReportNotFoundException(String.format("Report with report id %s not found.", reportId));
    }
    final byte[] reportContent = report.getReportString().getBytes(StandardCharsets.UTF_8);
    return getHttpEntity(reportContent);
  }

  /**
   * Computes and returns the latest version of the link checking report.
   * <p>
   * We can even invalidate it if we have a new execution (or we can check the most recent run starting time in the DB).
   *
   * @return The link checking report as a byte array (UTF-8 encoded).
   * @throws ClioException if an error occurred
   */
  @GetMapping(value = LATEST_REPORT_ENDPOINT_PATH, produces = {"text/csv", MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get full report with the latest link checking results.",
      description = "The links in the report may be part of multiple batches.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(mediaType = "text/csv"), @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
      @ApiResponse(responseCode = "404", description = "Report not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "500", description = "Persistence error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  public HttpEntity<byte[]> getLatestReport() throws ClioException {
    final Report report = reportingEngine.getLatestReports(1).stream().findFirst().orElse(null);
    if (report == null) {
      throw new ReportNotFoundException("Latest report not found.");
    }
    final byte[] reportContent = report.getReportString().getBytes(StandardCharsets.UTF_8);
    return getHttpEntity(reportContent);
  }

  /**
   * Get a historic overview of the most recent link checking batches.
   *
   * @param maxResults the maximum number of results to return
   * @return the most recent batches
   * @throws ClioException if an error occurred
   */
  @GetMapping(value = BATCHES_ENDPOINT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Get a historic overview of the most recent link checking batches.",
      description = "The batches are returned in reverse chronological order.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = @Content(schema = @Schema(implementation = BatchesRequestResult.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "500", description = "Persistence error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  public ResponseEntity<List<BatchesRequestResult>> getBatches(
      @RequestParam(value = "maxResults", required = false, defaultValue = "5")
      @Parameter(description = "The maximum number of batches returned, must be a positive number.", example = "1")
      int maxResults) throws ClioException {
    if (maxResults < 1) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    final List<BatchesRequestResult> result = this.reportingEngine
        .getLatestBatches(maxResults)
        .stream()
        .map(BatchesRequestResult::new)
        .toList();
    return new ResponseEntity<>(result, HttpStatus.OK);
  }


  /**
   * Get the run summary of the given {@link FilterRequest}.
   *
   * @param request the request
   * @return the run dataset summaries
   * @throws ClioException the clio exception
   */
  @PostMapping(value = RUNS_SUMMARY_ENDPOINT_PATH, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Returns a complete filtered view of Clio runs dataset summaries with pagination")
  @ApiResponse(responseCode = "400", description = "Filtering failed")
  public ResponseEntity<FilterResponse> findRunsSummary(
      @Parameter(description = "The filters to be applied", required = true) @Valid @RequestBody FilterRequest request)
      throws ClioException {
    if (request.getFilters() == null) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    // Sanitize filters before returning to prevent XSS injection of user-supplied filter values
    final FieldFilters sanitizedFilters = sanitizeFieldFilters(request.getFilters());
    final FieldFilters filterOptions = this.reportingEngine.findRunsSummaryFilterOptions(sanitizedFilters);
    final List<RunSummary> runSummaries = this.reportingEngine.findRunsSummary(sanitizedFilters);
    sanitizedFilters.setProvider(filterOptions.getProvider());
    sanitizedFilters.setDataProvider(filterOptions.getDataProvider());
    sanitizedFilters.setDatasetId(filterOptions.getDatasetId());
    sanitizedFilters.setDatasetName(filterOptions.getDatasetName());
    return new ResponseEntity<>( new FilterResponse(runSummaries, sanitizedFilters), HttpStatus.OK);
  }


  /**
   * Export the runs links matching the given {@link FilterRequest} as a CSV file.
   *
   * @param request the request
   * @return the response entity
   * @throws ClioException the clio exception
   */
  @PostMapping(value = RUNS_LINKS_EXPORT_ENDPOINT_PATH, produces = {"text/csv", MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Export filtered report of Clio runs dataset summaries with pagination",
      description = "The links in the report may be part of multiple runs.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "OK",
          content = {@Content(mediaType = "text/csv"), @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
      @ApiResponse(responseCode = "404", description = "Report not found",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE)),
      @ApiResponse(responseCode = "500", description = "Persistence error",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class),
              mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  public ResponseEntity<byte[]> exportRunsLinks(
      @Parameter(description = "The filters to be applied", required = true) @Valid @RequestBody FilterRequest request)
      throws ClioException {
    if (request == null || request.getFilters() == null) {
      return ResponseEntity.badRequest().build();
    }
    final FieldFilters sanitizedFilters = sanitizeFieldFilters(request.getFilters());
    final String report = reportingEngine.generateReport(sanitizedFilters);
    if (report == null) {
      throw new ReportNotFoundException("Report not found.");
    }
    final byte[] reportBytes = report.getBytes(StandardCharsets.UTF_8);
    return getHttpEntity(reportBytes);
  }
}
