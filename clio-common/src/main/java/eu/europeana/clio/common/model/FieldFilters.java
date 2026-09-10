package eu.europeana.clio.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * The type Clio filters.
 */
@JsonSerialize
@NoArgsConstructor
@Getter
@Setter
public class FieldFilters {

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
   * The provider filter is a set of strings, which means that they can be used to filter by multiple values at the same time. For
   * example, if the provider filter contains the values "provider1" and "provider2", the filtering will return all the records
   * that have either "provider1" or "provider2" as their provider.
   */
  @JsonProperty(FieldNames.PROVIDER)
  private SortedSet<String> provider;
  /**
   * The data provider filter is a set of strings, which means that it can be used to filter by multiple values at the same time.
   * For example, if the data provider filter contains the values "dataProvider1" and "dataProvider2", the filtering will return
   * all the records that have either "dataProvider1" or "dataProvider2" as their data provider.
   */
  @JsonProperty(FieldNames.DATA_PROVIDER)
  private SortedSet<String> dataProvider;
  /**
   * The dataset id filter is a set of strings, which means that it can be used to filter by multiple values at the same time. For
   * example, if the dataset id filter contains the values "datasetId1" and "datasetId2", the filtering will return all the
   * records that have either "datasetId1" or "datasetId2" as their dataset id.
   */
  @JsonProperty(FieldNames.DATASET_ID)
  private SortedSet<String> datasetId;
  /**
   * The dataset name filter is a set of strings, which means that it can be used to filter by multiple values at the same time.
   * For example, if the dataset name filter contains the values "datasetName1" and "datasetName2", the filtering will return all
   * the records that have either "datasetName1" or "datasetName2" as their dataset name.
   */
  @JsonProperty(FieldNames.DATASET_NAME)
  private SortedSet<String> datasetName;
  /**
   * The excluded check ids filter is a set of numbers, which means that it can be used to filter by multiple values at the same
   * time. For example, if the excluded check ids filter contains the values "checkId1" and "checkId2", the filtering will return
   * all the records that do not have either "checkId1" or "checkId2" as their check id.
   */
  @JsonProperty(FieldNames.EXCLUDED_DATASET_ID)
  private SortedSet<String> excludedDatasetId;
  /**
   * The date from and date to filters are dates, which means that they can be used to filter by a range of dates. For example, if
   * the date from filter is set to "2026-01-01" and the date to filter is set to "2026-12-31", the filtering will return all the
   * records that have a date between "2026-01-01" and "2026-12-31".
   */
  @JsonProperty(FieldNames.DATE_FROM)
  @Schema(pattern = "yyyy-MM-dd", example = "2026-01-01")
  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate dateFrom;
  /**
   * The date from and date to filters are dates, which means that they can be used to filter by a range of dates. For example, if
   * the date from filter is set to "2026-01-01" and the date to filter is set to "2026-12-31", the filtering will return all the
   * records that have a date between "2026-01-01" and "2026-12-31".
   */
  @JsonProperty(FieldNames.DATE_TO)
  @Schema(pattern = "yyyy-MM-dd", example = "2026-12-31")
  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate dateTo;
  /**
   * The percent links in operation FROM filters are integers, which means that they can be used to filter by a range of integers.
   * For example, if the percent links in operation FROM filter is set to 50 the filtering will return all the records that have a
   * percent links in operation inclusive and above 50.
   */
  @JsonProperty(FieldNames.PERCENT_LINKS_IN_OPERATION_FROM)
  private Integer percentLinksInOperationFrom;
  /**
   * The percent links in operation TO filters are integers, which means that they can be used to filter by a range of integers.
   * For example, if the percent links in operation TO filter is set to 100, the filtering will return all the records that have a
   * percent links in operation inclusive and under 100.
   */
  @JsonProperty(FieldNames.PERCENT_LINKS_IN_OPERATION_TO)
  private Integer percentLinksInOperationTo;
  /**
   * The offset filter is an integer, which means that they can be used to paginate the results.
   */
  @JsonProperty(FieldNames.OFFSET)
  @Schema(example = "0")
  private Integer offset;
  /**
   * The limit filter is an integer, which means that they can be used to filter the number of results returned.
   */
  @JsonProperty(FieldNames.LIMIT)
  @Schema(example = "5")
  private Integer limit;

  @JsonProperty(FieldNames.HAS_MORE_AVAILABLE)
  private boolean moreAvailable;

  /**
   * Instantiates a new Clio filter.
   *
   * @param provider the provider
   * @param dataProvider the data provider
   * @param datasetId the dataset id
   * @param datasetName the dataset name
   * @param excludedDatasetId the excluded check ids
   * @param dateFrom the date from
   * @param dateTo the date to
   * @param percentLinksInOperationFrom the percent links in operation from
   * @param percentLinksInOperationTo the percent links in operation to
   * @param offset the offset
   * @param limit the limit
   */
  @JsonCreator
  public FieldFilters(
      @JsonProperty(FieldNames.PROVIDER) SortedSet<String> provider,
      @JsonProperty(FieldNames.DATA_PROVIDER) SortedSet<String> dataProvider,
      @JsonProperty(FieldNames.DATASET_ID) SortedSet<String> datasetId,
      @JsonProperty(FieldNames.DATASET_NAME) SortedSet<String> datasetName,
      @JsonProperty(FieldNames.EXCLUDED_DATASET_ID) SortedSet<String> excludedDatasetId,
      @Schema(pattern = "yyyy-MM-dd")
      @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
      @JsonProperty(FieldNames.DATE_FROM) LocalDate dateFrom,
      @Schema(pattern = "yyyy-MM-dd")
      @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
      @JsonProperty(FieldNames.DATE_TO) LocalDate dateTo,
      @JsonProperty(FieldNames.PERCENT_LINKS_IN_OPERATION_FROM) Integer percentLinksInOperationFrom,
      @JsonProperty(FieldNames.PERCENT_LINKS_IN_OPERATION_TO) Integer percentLinksInOperationTo,
      @JsonProperty(FieldNames.OFFSET) Integer offset,
      @JsonProperty(FieldNames.LIMIT) Integer limit,
      @JsonProperty(FieldNames.HAS_MORE_AVAILABLE) Boolean moreAvailable) {
    this.dataProvider = dataProvider == null ? null : new TreeSet<>(dataProvider);
    this.provider = provider == null ? null : new TreeSet<>(provider);
    this.datasetId = datasetId == null ? null : new TreeSet<>(datasetId);
    this.datasetName = datasetName == null ? null : new TreeSet<>(datasetName);
    this.excludedDatasetId = excludedDatasetId == null ? null : new TreeSet<>(excludedDatasetId);
    this.dateFrom = dateFrom;
    this.dateTo = dateTo;
    this.percentLinksInOperationFrom = percentLinksInOperationFrom;
    this.percentLinksInOperationTo = percentLinksInOperationTo;
    this.offset = offset;
    this.limit = limit;
    this.moreAvailable = moreAvailable != null && moreAvailable;
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
        filters.getExcludedDatasetId(),             // No sanitization needed for numbers
        filters.getDateFrom(),                    // No sanitization needed for dates
        filters.getDateTo(),                      // No sanitization needed for dates
        filters.getPercentLinksInOperationFrom(), // No sanitization needed for range
        filters.getPercentLinksInOperationTo(),   // No sanitization needed for range
        sanitizeNumber(filters.getOffset()),
        sanitizeLimit(filters.getLimit()),
        filters.isMoreAvailable()
    );
  }

  /**
   * Escape HTML/XML special characters in a set of strings. Returns null if the input set is null, empty set if the input is
   * empty.
   *
   * @param stringSet the set of strings to sanitize
   * @return a new set with escaped strings
   */
  private static SortedSet<String> sanitizeStringSet(SortedSet<String> stringSet) {
    if (stringSet == null || stringSet.isEmpty()) {
      return stringSet;
    }
    return stringSet.stream()
                    .map(FieldFilters::escapeHtml)
                    .collect(Collectors.toCollection(TreeSet::new));
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
    if (value == null || value < 0) {
      return 0;
    }
    if (value < MIN_PAGE_LIMIT) {
      return MIN_PAGE_LIMIT;
    }
    if (value > MAX_PAGE_LIMIT) {
      return MAX_PAGE_LIMIT;
    }
    return value;
  }

  /**
   * Escape HTML/XML special characters to prevent XSS injection. Replaces: < > " ' & with their HTML entity equivalents.
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
