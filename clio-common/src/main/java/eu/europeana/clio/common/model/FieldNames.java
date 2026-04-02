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

  public static final String FILTERS = "filterOptions";
  public static final String PROVIDER = "provider";
  public static final String DATA_PROVIDER = "dataProvider";
  public static final String DATASET_ID = "datasetId";
  public static final String DATASET_NAME = "datasetName";
  public static final String DATASET_SIZE = "size";
  public static final String DATASET_LAST_INDEX = "lastIndexTime";
  public static final String EXCLUDED_CHECK_ID = "excludedCheckId";
  public static final String DATE_FROM = "dateFrom";
  public static final String DATE_TO = "dateTo";
  public static final String PERCENT_LINKS_IN_OPERATION_FROM = "percentLinksInOperationFrom";
  public static final String PERCENT_LINKS_IN_OPERATION_TO = "percentLinksInOperationTo";
  public static final String OFFSET = "offset";
  public static final String LIMIT = "limit";
  public static final String DATASET_NAME_DB = "name";
  public static final String DATASET_ID_DB = "datasetId";
  public static final String RUN_ID_DB = "runId";
  public static final String RECORD_ID_DB = "recordId";
  public static final String LINK_TYPE_DB = "linkType";
  public static final String LINK_URL_DB = "linkUrl";
  public static final String PERCENT_LINKS_IN_OPERATION_DB = "percentLinksInOperation";
  public static final String PERCENT_LINKS_IN_OPERATION_FROM_DB = "percentLinksInOperationFrom";
  public static final String PERCENT_LINKS_IN_OPERATION_TO_DB = "percentLinksInOperationTo";
  public static final String STARTING_TIME_DB = "startingTime";
  public static final String ENDING_TIME_DB = "endingTime";
  public static final String BATCH_ID_DB = "batchId";
  public static final String LINK_ID_DB = "linkId";
  public static final String ERROR_MESSAGE_DB = "error";
}
