package eu.europeana.clio.reporting.rest;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.reporting.core.ReportingEngine;

@Controller
public class ReportingController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReportingController.class);

  private final ReportingEngine reportingEngine;

  @Autowired
  public ReportingController(ReportingEngine reportingEngine) {
    this.reportingEngine = reportingEngine;
  }

  @GetMapping(value = "report", produces = "text/csv")
  @ResponseBody
  public HttpEntity<byte[]> getReport() throws ClioException, IOException {

    // Create the report.
    byte[] report;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final BufferedWriter outputWriter =
            new BufferedWriter(new OutputStreamWriter(outputStream))) {
      this.reportingEngine.generateReport(outputWriter);
      report = outputStream.toByteArray();
    } catch (ClioException | IOException | RuntimeException e) {
      LOGGER.warn("Problem while retrieving report.", e);
      throw e;
    }

    // Return the report.
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(
        ContentDisposition.builder("inline").filename("clio_report.csv").build());
    headers.setContentLength(report.length);
    return new HttpEntity<byte[]>(report, headers);
  }
}
