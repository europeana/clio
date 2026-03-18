package eu.europeana.clio.reporting.rest.controller;

import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.reporting.service.ReportingEngine;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * The type Controller utils.
 */
public final class ControllerUtils {

  /**
   * Utility class for controller-related helper methods, such as sanitizing user input to prevent XSS attacks.
   * This class is not meant to be instantiated, and all methods are static.
   */
  private ControllerUtils() {
    // Private constructor to prevent instantiation
  }

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

  /**
   * Sanitize FieldFilters to prevent XSS injection by escaping HTML/XML special characters in all string-based filter fields
   * before returning them to the client.
   *
   * @param filters the original filters from user input
   * @return a new FieldFilters object with sanitized string values
   */
  public static FieldFilters sanitizeFieldFilters(FieldFilters filters) {
    if (filters == null) {
      return null;
    }
    // Create a new FieldFilters object with sanitized string sets
    return new FieldFilters(
        sanitizeStringSet(filters.getProvider()),
        sanitizeStringSet(filters.getDataProvider()),
        sanitizeStringSet(filters.getDatasetId()),
        sanitizeStringSet(filters.getDatasetName()),
        filters.getExcludedCheckIds(),            // No sanitization needed for numbers
        filters.getDateFrom(),                    // No sanitization needed for dates
        filters.getDateTo(),                      // No sanitization needed for dates
        filters.getPercentLinksInOperationFrom(), // No sanitization needed for integers
        filters.getPercentLinksInOperationTo()    // No sanitization needed for integers
    );
  }

  /**
   * Escape HTML/XML special characters in a set of strings.
   * Returns null if the input set is null, empty set if the input is empty.
   *
   * @param stringSet the set of strings to sanitize
   * @return a new set with escaped strings
   */
  private static Set<String> sanitizeStringSet(Set<String> stringSet) {
    if (stringSet == null || stringSet.isEmpty()) {
      return stringSet;
    }
    return stringSet.stream()
                    .map(ControllerUtils::escapeHtml)
                    .collect(java.util.stream.Collectors.toSet());
  }

  /**
   * Escape HTML/XML special characters to prevent XSS injection.
   * Replaces: < > " ' & with their HTML entity equivalents.
   *
   * @param input the string to escape
   * @return the escaped string
   */
  private static String escapeHtml(String input) {
    if (input == null) {
      return null;
    }
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
