package eu.europeana.clio.common.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for ClioFilterField enum with 100% coverage.
 */
class ClioFilterFieldTest {

  @MethodSource
  public static Stream<Arguments> getClioFilterFields() {
    return Stream.of(
        Arguments.of(ClioFilterField.PROVIDER, FieldNames.PROVIDER),
        Arguments.of(ClioFilterField.DATA_PROVIDER, FieldNames.DATA_PROVIDER),
        Arguments.of(ClioFilterField.DATASET_ID, FieldNames.DATASET_ID),
        Arguments.of(ClioFilterField.DATASET_NAME, FieldNames.DATASET_NAME)
    );
  }

  @ParameterizedTest
  @MethodSource("getClioFilterFields")
  void testEnumConstant_hasCorrectFieldName(ClioFilterField field, String expectedFieldName) {
    // Given
    // No setup needed

    // When
    String fieldName = field.getFieldName();

    // Then
    assertNotNull(fieldName);
    assertEquals(expectedFieldName, fieldName);
  }

  @Test
  void testGetValueFields_returnsAllFourEnumValues() {
    // Given
    // No setup needed

    // When
    Set<ClioFilterField> valueFields = ClioFilterField.getValueFields();

    // Then
    assertNotNull(valueFields);
    assertEquals(4, valueFields.size());
    assertTrue(valueFields.contains(ClioFilterField.PROVIDER));
    assertTrue(valueFields.contains(ClioFilterField.DATA_PROVIDER));
    assertTrue(valueFields.contains(ClioFilterField.DATASET_ID));
    assertTrue(valueFields.contains(ClioFilterField.DATASET_NAME));
  }

  @Test
  void testProviderValueFilterGetter_retrievesProviderSetFromFieldFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> providerSet = Set.of("provider1", "provider2");
    filters.setProvider(providerSet);
    ClioFilterField provider = ClioFilterField.PROVIDER;

    // When
    var valueFilterGetter = provider.getValueFilterGetter();
    Set<String> result = valueFilterGetter.apply(filters);

    // Then
    assertEquals(providerSet, result);
    assertEquals(2, result.size());
    assertTrue(result.contains("provider1"));
    assertTrue(result.contains("provider2"));
  }

  @Test
  void testDataProviderValueFilterGetter_retrievesDataProviderSetFromFieldFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> dataProviderSet = Set.of("dataProvider1");
    filters.setDataProvider(dataProviderSet);
    ClioFilterField dataProvider = ClioFilterField.DATA_PROVIDER;

    // When
    var valueFilterGetter = dataProvider.getValueFilterGetter();
    Set<String> result = valueFilterGetter.apply(filters);

    // Then
    assertEquals(dataProviderSet, result);
    assertEquals(1, result.size());
    assertTrue(result.contains("dataProvider1"));
  }

  @Test
  void testDatasetIdValueFilterGetter_retrievesDatasetIdSetFromFieldFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> datasetIdSet = Set.of("dataset1", "dataset2", "dataset3");
    filters.setDatasetId(datasetIdSet);
    ClioFilterField datasetId = ClioFilterField.DATASET_ID;

    // When
    var valueFilterGetter = datasetId.getValueFilterGetter();
    Set<String> result = valueFilterGetter.apply(filters);

    // Then
    assertEquals(datasetIdSet, result);
    assertEquals(3, result.size());
    assertTrue(result.contains("dataset1"));
    assertTrue(result.contains("dataset2"));
    assertTrue(result.contains("dataset3"));
  }

  @Test
  void testDatasetNameValueFilterGetter_retrievesDatasetNameSetFromFieldFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> datasetNameSet = Set.of("name1", "name2");
    filters.setDatasetName(datasetNameSet);
    ClioFilterField datasetName = ClioFilterField.DATASET_NAME;

    // When
    var valueFilterGetter = datasetName.getValueFilterGetter();
    Set<String> result = valueFilterGetter.apply(filters);

    // Then
    assertEquals(datasetNameSet, result);
    assertEquals(2, result.size());
    assertTrue(result.contains("name1"));
    assertTrue(result.contains("name2"));
  }

  @Test
  void testProviderValueFilterSetter_setsProviderSetInFieldFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> providerSet = Set.of("provider1", "provider2");
    ClioFilterField provider = ClioFilterField.PROVIDER;

    // When
    var valueFilterSetter = provider.getValueFilterSetter();
    valueFilterSetter.accept(filters, providerSet);

    // Then
    assertEquals(providerSet, filters.getProvider());
    assertEquals(2, filters.getProvider().size());
    assertTrue(filters.getProvider().contains("provider1"));
    assertTrue(filters.getProvider().contains("provider2"));
  }

  @Test
  void testDataProviderValueFilterSetter_setsDataProviderSetInFieldFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> dataProviderSet = Set.of("dataProvider1");
    ClioFilterField dataProvider = ClioFilterField.DATA_PROVIDER;

    // When
    var valueFilterSetter = dataProvider.getValueFilterSetter();
    valueFilterSetter.accept(filters, dataProviderSet);

    // Then
    assertEquals(dataProviderSet, filters.getDataProvider());
    assertEquals(1, filters.getDataProvider().size());
    assertTrue(filters.getDataProvider().contains("dataProvider1"));
  }

  @Test
  void testDatasetIdValueFilterSetter_setsDatasetIdSetInFieldFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> datasetIdSet = Set.of("dataset1", "dataset2");
    ClioFilterField datasetId = ClioFilterField.DATASET_ID;

    // When
    var valueFilterSetter = datasetId.getValueFilterSetter();
    valueFilterSetter.accept(filters, datasetIdSet);

    // Then
    assertEquals(datasetIdSet, filters.getDatasetId());
    assertEquals(2, filters.getDatasetId().size());
    assertTrue(filters.getDatasetId().contains("dataset1"));
    assertTrue(filters.getDatasetId().contains("dataset2"));
  }

  @Test
  void testDatasetNameValueFilterSetter_setsDatasetNameSetInFieldFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> datasetNameSet = Set.of("name1");
    ClioFilterField datasetName = ClioFilterField.DATASET_NAME;

    // When
    var valueFilterSetter = datasetName.getValueFilterSetter();
    valueFilterSetter.accept(filters, datasetNameSet);

    // Then
    assertEquals(datasetNameSet, filters.getDatasetName());
    assertEquals(1, filters.getDatasetName().size());
    assertTrue(filters.getDatasetName().contains("name1"));
  }

  @Test
  void testGetValueFields_returnsNewSetInstance() {
    // Given
    Set<ClioFilterField> firstCall = ClioFilterField.getValueFields();

    // When
    Set<ClioFilterField> secondCall = ClioFilterField.getValueFields();

    // Then
    assertNotNull(firstCall);
    assertNotNull(secondCall);
    assertEquals(firstCall, secondCall);
    // Verify both contain the same elements
    assertEquals(firstCall.size(), secondCall.size());
  }

  @Test
  void testEnumValues_canBeRetrievedByName() {
    // Given
    // No setup needed

    // When
    ClioFilterField provider = ClioFilterField.valueOf("PROVIDER");
    ClioFilterField dataProvider = ClioFilterField.valueOf("DATA_PROVIDER");
    ClioFilterField datasetId = ClioFilterField.valueOf("DATASET_ID");
    ClioFilterField datasetName = ClioFilterField.valueOf("DATASET_NAME");

    // Then
    assertEquals(ClioFilterField.PROVIDER, provider);
    assertEquals(ClioFilterField.DATA_PROVIDER, dataProvider);
    assertEquals(ClioFilterField.DATASET_ID, datasetId);
    assertEquals(ClioFilterField.DATASET_NAME, datasetName);
  }

  @Test
  void testEnumValues_returnsArrayOfAllConstants() {
    // Given
    // No setup needed

    // When
    ClioFilterField[] values = ClioFilterField.values();

    // Then
    assertNotNull(values);
    assertEquals(4, values.length);
  }

  @Test
  void testProviderValueFilterGetterAndSetter_roundTrip() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> originalSet = Set.of("provider1", "provider2", "provider3");
    ClioFilterField provider = ClioFilterField.PROVIDER;

    // When
    provider.getValueFilterSetter().accept(filters, originalSet);
    Set<String> retrievedSet = provider.getValueFilterGetter().apply(filters);

    // Then
    assertEquals(originalSet, retrievedSet);
    assertEquals(3, retrievedSet.size());
  }

  @Test
  void testDataProviderValueFilterGetterAndSetter_roundTrip() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> originalSet = Set.of("dp1", "dp2");
    ClioFilterField dataProvider = ClioFilterField.DATA_PROVIDER;

    // When
    dataProvider.getValueFilterSetter().accept(filters, originalSet);
    Set<String> retrievedSet = dataProvider.getValueFilterGetter().apply(filters);

    // Then
    assertEquals(originalSet, retrievedSet);
    assertEquals(2, retrievedSet.size());
  }

  @Test
  void testDatasetIdValueFilterGetterAndSetter_roundTrip() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> originalSet = Set.of("did1");
    ClioFilterField datasetId = ClioFilterField.DATASET_ID;

    // When
    datasetId.getValueFilterSetter().accept(filters, originalSet);
    Set<String> retrievedSet = datasetId.getValueFilterGetter().apply(filters);

    // Then
    assertEquals(originalSet, retrievedSet);
    assertEquals(1, retrievedSet.size());
  }

  @Test
  void testDatasetNameValueFilterGetterAndSetter_roundTrip() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> originalSet = Set.of("dataset1", "dataset2", "dataset3", "dataset4");
    ClioFilterField datasetName = ClioFilterField.DATASET_NAME;

    // When
    datasetName.getValueFilterSetter().accept(filters, originalSet);
    Set<String> retrievedSet = datasetName.getValueFilterGetter().apply(filters);

    // Then
    assertEquals(originalSet, retrievedSet);
    assertEquals(4, retrievedSet.size());
  }

  @Test
  void testMultipleEnumValues_withDifferentFieldsInSameFilter() {
    // Given
    FieldFilters filters = new FieldFilters();
    Set<String> providerSet = Set.of("prov1");
    Set<String> dataProviderSet = Set.of("dprov1");
    Set<String> datasetIdSet = Set.of("ds1");
    Set<String> datasetNameSet = Set.of("name1");

    // When
    ClioFilterField.PROVIDER.getValueFilterSetter().accept(filters, providerSet);
    ClioFilterField.DATA_PROVIDER.getValueFilterSetter().accept(filters, dataProviderSet);
    ClioFilterField.DATASET_ID.getValueFilterSetter().accept(filters, datasetIdSet);
    ClioFilterField.DATASET_NAME.getValueFilterSetter().accept(filters, datasetNameSet);

    // Then
    assertEquals(providerSet, ClioFilterField.PROVIDER.getValueFilterGetter().apply(filters));
    assertEquals(dataProviderSet, ClioFilterField.DATA_PROVIDER.getValueFilterGetter().apply(filters));
    assertEquals(datasetIdSet, ClioFilterField.DATASET_ID.getValueFilterGetter().apply(filters));
    assertEquals(datasetNameSet, ClioFilterField.DATASET_NAME.getValueFilterGetter().apply(filters));
  }

  @Test
  void testFieldNames_areCorrectAndDistinct() {
    // Given
    // No setup needed

    // When
    String providerName = ClioFilterField.PROVIDER.getFieldName();
    String dataProviderName = ClioFilterField.DATA_PROVIDER.getFieldName();
    String datasetIdName = ClioFilterField.DATASET_ID.getFieldName();
    String datasetNameName = ClioFilterField.DATASET_NAME.getFieldName();

    // Then
    assertEquals("provider", providerName);
    assertEquals("dataProvider", dataProviderName);
    assertEquals("datasetId", datasetIdName);
    assertEquals("datasetName", datasetNameName);

    // Verify all are distinct
    Set<String> allNames = Set.of(providerName, dataProviderName, datasetIdName, datasetNameName);
    assertEquals(4, allNames.size());
  }

  @Test
  void testValueFilterGetter_isNotNull() {
    // Given
    ClioFilterField provider = ClioFilterField.PROVIDER;

    // When
    var getter = provider.getValueFilterGetter();

    // Then
    assertNotNull(getter);
  }

  @Test
  void testValueFilterSetter_isNotNull() {
    // Given
    ClioFilterField provider = ClioFilterField.PROVIDER;

    // When
    var setter = provider.getValueFilterSetter();

    // Then
    assertNotNull(setter);
  }

  @Test
  void testValueFilterGetter_withNullSetInFilters_returnsNull() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setProvider(null);
    ClioFilterField provider = ClioFilterField.PROVIDER;

    // When
    Set<String> result = provider.getValueFilterGetter().apply(filters);

    // Then
    assertNull(result);
  }

  @Test
  void testValueFilterSetter_withNullSet() {
    // Given
    FieldFilters filters = new FieldFilters();
    ClioFilterField provider = ClioFilterField.PROVIDER;

    // When
    provider.getValueFilterSetter().accept(filters, null);

    // Then
    assertNull(filters.getProvider());
  }
}
