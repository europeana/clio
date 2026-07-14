package eu.europeana.clio.common.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;


/**
 * The enum Clio filter field.
 */
public enum ClioFilterField {

  /**
   * Provider clio filter field.
   */
  PROVIDER(FieldNames.PROVIDER, FieldFilters::getProvider, FieldFilters::setProvider),
  /**
   * Data provider clio filter field.
   */
  DATA_PROVIDER(FieldNames.DATA_PROVIDER, FieldFilters::getDataProvider, FieldFilters::setDataProvider),
  /**
   * Dataset id clio filter field.
   */
  DATASET_ID(FieldNames.DATASET_ID, FieldFilters::getDatasetId, FieldFilters::setDatasetId),
  /**
   * Dataset name clio filter field.
   */
  DATASET_NAME(FieldNames.DATASET_NAME, FieldFilters::getDatasetName, FieldFilters::setDatasetName);

  private static final Integer CLIO_FILTER_FIELD_COUNT = 4;
  private final String fieldName;
  private final Function<FieldFilters, Set<String>> valueFilterGetter;
  private final BiConsumer<FieldFilters, Set<String>> valueFilterSetter;

  ClioFilterField(String fieldName,
      Function<FieldFilters, Set<String>> valueFilterGetter,
      BiConsumer<FieldFilters, Set<String>> valueFilterSetter) {
    this.fieldName = fieldName;
    this.valueFilterGetter = valueFilterGetter;
    this.valueFilterSetter = valueFilterSetter;
  }

  /**
   * Gets value fields.
   *
   * @return the value fields
   */
  public static Set<ClioFilterField> getValueFields() {
    Set<ClioFilterField> result = HashSet.newHashSet(CLIO_FILTER_FIELD_COUNT);
    result.add(ClioFilterField.PROVIDER);
    result.add(ClioFilterField.DATA_PROVIDER);
    result.add(ClioFilterField.DATASET_NAME);
    result.add(ClioFilterField.DATASET_ID);

    return result;
  }

  /**
   * Gets field name.
   *
   * @return the field name
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Gets value filter getter.
   *
   * @return the value filter getter
   */
  public Function<FieldFilters, Set<String>> getValueFilterGetter() {
    return valueFilterGetter;
  }

  /**
   * Gets value filter setter.
   *
   * @return the value filter setter
   */
  public BiConsumer<FieldFilters, Set<String>> getValueFilterSetter() {
    return valueFilterSetter;
  }

}
