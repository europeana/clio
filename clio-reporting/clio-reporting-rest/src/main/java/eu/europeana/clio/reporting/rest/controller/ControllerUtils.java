package eu.europeana.clio.reporting.rest.controller;

import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.reporting.service.ReportingEngine;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for controller-related helper methods, such as sanitizing user input to prevent XSS attacks. This class is not
 * meant to be instantiated, and all methods are static.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ControllerUtils {



  /**
   * Gets http entity.
   *
   * @param reportBytes the report bytes
   * @return the http entity
   */
  public static ResponseEntity<byte[]> getHttpEntity(byte[] reportBytes) {
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(
        ContentDisposition.builder("inline").filename(ReportingEngine.getReportFileNameSuggestion()).build());
    headers.setContentLength(reportBytes.length);
    headers.setContentType(MediaType.valueOf("text/csv"));
    return ResponseEntity.ok().headers(headers).body(reportBytes);
  }


}
