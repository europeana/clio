package eu.europeana.clio.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The type Pagination.
 */
public record Pagination(@JsonProperty(FieldNames.OFFSET) @Schema(example = "0") Integer offset,
                         @JsonProperty(FieldNames.LIMIT) @Schema(example = "5") Integer limit,
                         @JsonProperty(FieldNames.HAS_MORE_AVAILABLE) Boolean moreAvailable) {

  /**
   * The minimum and maximum page limits. These constants are used to ensure that the limit filter is within a reasonable range,
   * preventing potential performance issues or abuse of the API by requesting too many records at once.
   */
  private static final int MIN_PAGE_LIMIT = 5;
  /**
   * The minimum and maximum page limits. These constants are used to ensure that the limit filter is within a reasonable range,
   * preventing potential performance issues or abuse of the API by requesting too many records at once.
   */
  private static final int MAX_PAGE_LIMIT = 100;

  /**
   * Instantiates a new Pagination.
   *
   * @param offset the offset
   * @param limit the limit
   * @param moreAvailable the more available
   */
  public Pagination(Integer offset, Integer limit, Boolean moreAvailable) {
    this.offset = sanitizeNumber(offset);
    this.limit = sanitizeLimit(limit);
    this.moreAvailable = moreAvailable != null && moreAvailable;
  }

  /**
   * Sanitize number integer.
   *
   * @param value the value
   * @return the integer
   */
  private static Integer sanitizeNumber(Integer value) {
    if (value == null || value < 0) {
      return 0;
    }
    return value;
  }

  /**
   * Sanitize limit integer.
   *
   * @param value the value
   * @return the integer
   */
  private static Integer sanitizeLimit(Integer value) {
    if (value == null || value < MIN_PAGE_LIMIT) {
      return MIN_PAGE_LIMIT;
    }
    if (value > MAX_PAGE_LIMIT) {
      return MAX_PAGE_LIMIT;
    }
    return value;
  }
}
