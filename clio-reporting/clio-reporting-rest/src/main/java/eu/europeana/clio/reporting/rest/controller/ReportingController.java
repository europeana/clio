package eu.europeana.clio.reporting.rest.controller;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.exception.ReportNotFoundException;
import eu.europeana.clio.common.model.Report;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
     * Get all available report details.
     *
     * @return the report details
     * @throws ClioException if an error occurred
     * @throws PersistenceException if there was an error while getting the report details
     */
    @GetMapping(value = "/available-reports", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all available report details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = ReportDetailsView.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Persistence error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<ReportDetailsView>> availableReports() throws ClioException {
        final List<Report> allReports = reportingEngine.getAllReportDetails();
        final List<ReportDetailsView> reportDetailsViews = allReports.stream().map(
                report -> {
                    Instant instant = Instant.ofEpochMilli(report.getCreationTime());
                    final String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/report-by-creation-time").queryParam("creationTime", instant).toUriString();
                    return new ReportDetailsView(report.getReportId(), report.getBatchId(), instant, url);
                }).collect(Collectors.toList());

        return new ResponseEntity<>(reportDetailsViews, HttpStatus.OK);


    }

    /**
     * Get a report by providing its creation time
     *
     * @param creationTime the creation time
     * @return the report
     * @throws ClioException if an error occurred
     * @throws ReportNotFoundException if the report was not found
     * @throws PersistenceException    if there was an error while getting the report
     */
    @GetMapping(value = "report-by-creation-time", produces = {"text/csv", MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @Operation(summary = "Get a report by creation timestamp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = {@Content(mediaType = "text/csv, "+ MediaType.APPLICATION_JSON_VALUE)}),
            @ApiResponse(responseCode = "404", description = "Report not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Persistence error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public HttpEntity<byte[]> getReportByCreationTime(@RequestParam(value = "creationTime") Instant creationTime)
            throws ClioException {
        Report report = reportingEngine.getReportByCreationTime(creationTime);
        return getHttpEntity(report);
    }

    /**
     * Computes and returns the latest version of the link checking report.
     * <p>
     * We can even invalidate it if we have a new execution (or we can check the most recent run
     * starting time in the DB).
     *
     * @return The link checking report as a byte array (UTF-8 encoded).
     * @throws ClioException if an error occurred
     * @throws ReportNotFoundException if the report was not found
     * @throws PersistenceException    if there was an error while getting the report
     */
    @GetMapping(value = "latest-report", produces = {"text/csv", MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @Operation(summary = "Get full report with the the latest link checking results.", description = "The links in the report may be part of multiple batches.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = {@Content(mediaType = "text/csv, "+ MediaType.APPLICATION_JSON_VALUE)}),
            @ApiResponse(responseCode = "404", description = "Report not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Persistence error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public HttpEntity<byte[]> getLatestReport() throws ClioException {
        final Report report = reportingEngine.getLatestReports(1).stream().findFirst().orElse(null);
        return getHttpEntity(report);
    }

    private HttpEntity<byte[]> getHttpEntity(Report report) throws ReportNotFoundException {
        if (report == null) {
            throw new ReportNotFoundException();
        } else {
            byte[] reportBytes = report.getReportString().getBytes(StandardCharsets.UTF_8);
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.builder("inline")
                    .filename(ReportingEngine.getReportFileNameSuggestion(Instant.ofEpochMilli(report.getCreationTime()))).build());
            headers.setContentLength(reportBytes.length);
            return new HttpEntity<>(reportBytes, headers);
        }
    }

    /**
     * Get a historic overview of the most recent link checking batches.
     *
     * @param maxResults the maximum number of results to return
     * @return the most recent batches
     * @throws ClioException if an error occurred
     * @throws PersistenceException if there was an error while getting the batches
     */
    @GetMapping(value = "batches", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(summary =
            "Get a historic overview of the most recent link checking batches.", description = "The batches are returned in reverse chronological order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = BatchesRequestResult.class), mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "500", description = "Persistence error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class), mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    public ResponseEntity<List<BatchesRequestResult>> getBatches(
            @RequestParam(value = "maxResults", required = false, defaultValue = "5")
            @Parameter(description = "The maximum number of batches returned, must be a positive number.", example = "1")
            int maxResults) throws ClioException {
        if (maxResults < 1) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final List<BatchesRequestResult> result = this.reportingEngine.getLatestBatches(maxResults)
                .stream().map(BatchesRequestResult::new).collect(Collectors.toList());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
