package eu.europeana.clio.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.Set;
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
   * The provider filter is a set of strings, which means that they can be used to filter by multiple values at the same time.
   * For example, if the provider filter contains the values "provider1" and "provider2",
   * the filtering will return all the records that have either "provider1" or "provider2" as their provider.
   */
  @JsonProperty(FieldNames.PROVIDER)
  private Set<String> provider;
  /**
   * The data provider filter is a set of strings, which means that it can be used to filter by multiple values at the same time.
   * For example, if the data provider filter contains the values "dataProvider1" and "dataProvider2",
   * the filtering will return all the records that have either "dataProvider1" or "dataProvider2" as their data provider.
   */
  @JsonProperty(FieldNames.DATA_PROVIDER)
  private Set<String> dataProvider;
  /**
   * The dataset id filter is a set of strings, which means that it can be used to filter by multiple values at the same time.
   * For example, if the dataset id filter contains the values "datasetId1" and "datasetId2",
   * the filtering will return all the records that have either "datasetId1" or "datasetId2" as their dataset id.
   */
  @JsonProperty(FieldNames.DATASET_ID)
  private Set<String> datasetId;
  /**
   * The dataset name filter is a set of strings, which means that it can be used to filter by multiple values at the same time.
   * For example, if the dataset name filter contains the values "datasetName1" and "datasetName2",
   * the filtering will return all the records that have either "datasetName1" or "datasetName2" as their dataset name.
   */
  @JsonProperty(FieldNames.DATASET_NAME)
  private Set<String> datasetName;
  /**
   * The excluded check ids filter is a set of numbers, which means that it can be used to filter by multiple values at the same time.
   * For example, if the excluded check ids filter contains the values "checkId1" and "checkId2",
   * the filtering will return all the records that do not have either "checkId1" or "checkId2" as their check id.
   */
  @JsonProperty(FieldNames.EXCLUDED_CHECK_IDS)
  private Set<Long> excludedCheckIds;
  /**
   * The date from and date to filters are dates, which means that they can be used to filter by a range of dates.
   * For example, if the date from filter is set to "2026-01-01" and the date to filter is set to "2026-12-31",
   * the filtering will return all the records that have a date between "2026-01-01" and "2026-12-31".
   */
  @JsonProperty(FieldNames.DATE_FROM)
  @Schema(pattern = "yyyy-MM-dd", example = "2026-01-01")
  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
  private Date dateFrom;
  /**
   * The date from and date to filters are dates, which means that they can be used to filter by a range of dates.
   * For example, if the date from filter is set to "2026-01-01" and the date to filter is set to "2026-12-31",
   * the filtering will return all the records that have a date between "2026-01-01" and "2026-12-31".
   */
  @JsonProperty(FieldNames.DATE_TO)
  @Schema(pattern = "yyyy-MM-dd", example = "2026-12-31")
  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
  private Date dateTo;
  /**
   * The percent links in operation FROM filters are integers,
   * which means that they can be used to filter by a range of integers.
   * For example, if the percent links in operation FROM filter is set to 50
   * the filtering will return all the records that have a percent links in operation inclusive and above 50.
   */
  @JsonProperty(FieldNames.PERCENT_LINKS_IN_OPERATION_FROM)
  private Integer percentLinksInOperationFrom;
  /**
   * The percent links in operation TO filters are integers,
   * which means that they can be used to filter by a range of integers.
   * For example, if the percent links in operation TO filter is set to 100,
   * the filtering will return all the records that have a percent links in operation inclusive and under 100.
   */
  @JsonProperty(FieldNames.PERCENT_LINKS_IN_OPERATION_TO)
  private Integer percentLinksInOperationTo;

  /**
   * Instantiates a new Clio filters.
   *
   * @param provider the provider
   * @param dataProvider the data provider
   * @param datasetId the dataset id
   * @param datasetName the dataset name
   * @param excludedCheckIds the excluded check ids
   * @param dateFrom the date from
   * @param dateTo the date to
   * @param percentLinksInOperationFrom the percent links in operation from
   * @param percentLinksInOperationTo the percent links in operation to
   */
  @JsonCreator
  public FieldFilters(
      @JsonProperty(FieldNames.PROVIDER) Set<String> provider,
      @JsonProperty(FieldNames.DATA_PROVIDER) Set<String> dataProvider,
      @JsonProperty(FieldNames.DATASET_ID) Set<String> datasetId,
      @JsonProperty(FieldNames.DATASET_NAME) Set<String> datasetName,
      @JsonProperty(FieldNames.EXCLUDED_CHECK_IDS) Set<Long> excludedCheckIds,
      @Schema(pattern = "yyyy-MM-dd")
      @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
      @JsonProperty(FieldNames.DATE_FROM) Date dateFrom,
      @Schema(pattern = "yyyy-MM-dd")
      @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
      @JsonProperty(FieldNames.DATE_TO) Date dateTo,
      @JsonProperty(FieldNames.PERCENT_LINKS_IN_OPERATION_FROM) Integer percentLinksInOperationFrom,
      @JsonProperty(FieldNames.PERCENT_LINKS_IN_OPERATION_TO) Integer percentLinksInOperationTo) {
    this.dataProvider = dataProvider;
    this.provider = provider;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.excludedCheckIds = excludedCheckIds;
    this.dateFrom = dateFrom;
    this.dateTo = dateTo;
    this.percentLinksInOperationFrom = percentLinksInOperationFrom;
    this.percentLinksInOperationTo = percentLinksInOperationTo;
  }
}
