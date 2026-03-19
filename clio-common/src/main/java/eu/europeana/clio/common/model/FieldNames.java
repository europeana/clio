package eu.europeana.clio.common.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class containing the type Field names constants.
 * These are used in the code to avoid hardcoding field names in multiple places.
 * This class is not meant to be instantiated, hence the private constructor.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FieldNames {

  /**
   * The constant FILTERS.
   */
  public static final String FILTERS = "filters";
  /**
   * The constant PROVIDER.
   */
  public static final String PROVIDER = "provider";
  /**
   * The constant DATA_PROVIDER.
   */
  public static final String DATA_PROVIDER = "dataProvider";
  /**
   * The constant DATASET_ID.
   */
  public static final String DATASET_ID = "datasetId";
  /**
   * The constant DATASET_NAME.
   */
  public static final String DATASET_NAME = "datasetName";
  /**
   * The constant EXCLUDED_CHECK_IDS.
   */
  public static final String EXCLUDED_CHECK_IDS = "excludedCheckIds";
  /**
   * The constant DATE_FROM.
   */
  public static final String DATE_FROM = "dateFrom";
  /**
   * The constant DATE_TO.
   */
  public static final String DATE_TO = "dateTo";
  /**
   * The constant PERCENT_LINKS_IN_OPERATION_FROM.
   */
  public static final String PERCENT_LINKS_IN_OPERATION_FROM = "percentLinksInOperationFrom";
  /**
   * The constant PERCENT_LINKS_IN_OPERATION_TO.
   */
  public static final String PERCENT_LINKS_IN_OPERATION_TO = "percentLinksInOperationTo";
  /**
   * The constant DATASET_NAME_DB.
   */
  public static final String DATASET_NAME_DB = "name";
  /**
   * The constant DATASET_ID_DB.
   */
  public static final String DATASET_ID_DB = "datasetId";
  /**
   * The constant RUN_ID_DB.
   */
  public static final String RUN_ID_DB = "runId";
  /**
   * The constant RECORD_ID_DB.
   */
  public static final String RECORD_ID_DB = "recordId";
  /**
   * The constant LINK_TYPE_DB.
   */
  public static final String LINK_TYPE_DB = "linkType";
  /**
   * The constant LINK_URL_DB.
   */
  public static final String LINK_URL_DB = "linkUrl";
  /**
   * The constant TOTAL_LINKS_DB.
   */
  public static final String TOTAL_LINKS_DB = "totalLinks";
  /**
   * The constant ERROR_LINKS_DB.
   */
  public static final String ERROR_LINKS_DB = "errorsLinks";
  /**
   * The constant STARTING_TIME_DB.
   */
  public static final String STARTING_TIME_DB = "startingTime";
  /**
   * The constant ENDING_TIME_DB.
   */
  public static final String ENDING_TIME_DB = "endingTime";
  /**
   * The constant BATCH_ID_DB.
   */
  public static final String BATCH_ID_DB = "batchId";
  /**
   * The constant LINK_ID_DB.
   */
  public static final String LINK_ID_DB = "linkId";
  /**
   * The constant ERROR_MESSAGE_DB.
   */
  public static final String ERROR_MESSAGE_DB = "error";
}
