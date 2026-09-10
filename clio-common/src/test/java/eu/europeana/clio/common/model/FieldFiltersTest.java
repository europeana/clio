package eu.europeana.clio.common.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class FieldFiltersTest {

  @Test
  void testConstructor_withAllParameters_createsInstanceSuccessfully() {
    // Given
    SortedSet<String> provider = new TreeSet<>(Set.of("provider1", "provider2"));
    SortedSet<String> dataProvider = new TreeSet<>(Set.of("dataProvider1"));
    SortedSet<String> datasetId = new TreeSet<>(Set.of("dataset1", "dataset2"));
    SortedSet<String> datasetName = new TreeSet<>(Set.of("name1"));
    SortedSet<String> excludedCheckId = new TreeSet<>(Set.of("dataset3"));
    LocalDate dateFrom = LocalDate.now(ZoneOffset.UTC);
    LocalDate dateTo = LocalDate.now(ZoneOffset.UTC).plus(1, ChronoUnit.DAYS);
    Integer percentLinksInOperationFrom = 10;
    Integer percentLinksInOperationTo = 90;
    Integer offset = 0;
    Integer limit = 50;

    // When
    FieldFilters fieldFilters = new FieldFilters(
        provider, dataProvider, datasetId, datasetName, excludedCheckId,
        dateFrom, dateTo, percentLinksInOperationFrom, percentLinksInOperationTo,
        offset, limit, false
    );

    // Then
    assertNotNull(fieldFilters);
    assertEquals(provider, fieldFilters.getProvider());
    assertEquals(dataProvider, fieldFilters.getDataProvider());
    assertEquals(datasetId, fieldFilters.getDatasetId());
    assertEquals(datasetName, fieldFilters.getDatasetName());
    assertEquals(excludedCheckId, fieldFilters.getExcludedDatasetId());
    assertEquals(dateFrom, fieldFilters.getDateFrom());
    assertEquals(dateTo, fieldFilters.getDateTo());
    assertEquals(percentLinksInOperationFrom, fieldFilters.getPercentLinksInOperationFrom());
    assertEquals(percentLinksInOperationTo, fieldFilters.getPercentLinksInOperationTo());
    assertEquals(offset, fieldFilters.getOffset());
    assertEquals(limit, fieldFilters.getLimit());
  }

  @Test
  void testConstructor_withNullParameters_createsInstanceSuccessfully() {
    // Given
    // All parameters are null

    // When
    FieldFilters fieldFilters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null,
        null, null, false
    );

    // Then
    assertNotNull(fieldFilters);
    assertNull(fieldFilters.getProvider());
    assertNull(fieldFilters.getDataProvider());
    assertNull(fieldFilters.getDatasetId());
    assertNull(fieldFilters.getDatasetName());
    assertNull(fieldFilters.getExcludedDatasetId());
    assertNull(fieldFilters.getDateFrom());
    assertNull(fieldFilters.getDateTo());
    assertNull(fieldFilters.getPercentLinksInOperationFrom());
    assertNull(fieldFilters.getPercentLinksInOperationTo());
    assertNull(fieldFilters.getOffset());
    assertNull(fieldFilters.getLimit());
  }

  @Test
  void testConstructor_withProviderSet_createsCopyOfSet() {
    // Given
    SortedSet<String> provider = new TreeSet<>(Set.of("provider1", "provider2"));

    // When
    FieldFilters fieldFilters = new FieldFilters(
        provider, null, null, null, null,
        null, null, null, null,
        null, null, false
    );

    // Then
    assertNotNull(fieldFilters.getProvider());
    assertEquals(provider, fieldFilters.getProvider());
    // Verify it contains the expected values
    assertEquals(2, fieldFilters.getProvider().size());
    assertTrue(fieldFilters.getProvider().contains("provider1"));
    assertTrue(fieldFilters.getProvider().contains("provider2"));
  }

  @Test
  void testConstructor_withExcludedCheckIdSet_createsCopyOfSet() {
    // Given
    SortedSet<String> excludedCheckId = new TreeSet<>(Set.of("dataset1","dataset2","dataset3"));

    // When
    FieldFilters fieldFilters = new FieldFilters(
        null, null, null, null, excludedCheckId,
        null, null, null, null,
        null, null, false
    );

    // Then
    assertNotNull(fieldFilters.getExcludedDatasetId());
    assertEquals(excludedCheckId, fieldFilters.getExcludedDatasetId());
  }

  @Test
  void testSanitizeFieldFilters_withNullInput_returnsNull() {
    // Given
    FieldFilters filters = null;

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNull(sanitized);
  }

  @Test
  void testSanitizeFieldFilters_withXSSInjectionInProvider_escapesHtmlCharacters() {
    // Given
    SortedSet<String> provider = new TreeSet<>(Set.of("<script>alert('xss')</script>", "provider&test"));
    FieldFilters filters = new FieldFilters(
        provider, null, null, null, null,
        null, null, null, null, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertNotNull(sanitized.getProvider());
    assertTrue(sanitized.getProvider().contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"));
    assertTrue(sanitized.getProvider().contains("provider&amp;test"));
  }

  @Test
  void testSanitizeFieldFilters_withXSSInjectionInDataProvider_escapesHtmlCharacters() {
    // Given
    SortedSet<String> dataProvider = new TreeSet<>(Set.of("provider<tag>", "provider\"quoted\""));
    FieldFilters filters = new FieldFilters(
        null, dataProvider, null, null, null,
        null, null, null, null, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertNotNull(sanitized.getDataProvider());
    assertTrue(sanitized.getDataProvider().contains("provider&lt;tag&gt;"));
    assertTrue(sanitized.getDataProvider().contains("provider&quot;quoted&quot;"));
  }

  @Test
  void testSanitizeFieldFilters_withNegativeOffset_setsToZero() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null, -10, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(0, sanitized.getOffset());
  }

  @Test
  void testSanitizeFieldFilters_withNullOffset_setsToZero() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(0, sanitized.getOffset());
  }

  @Test
  void testSanitizeFieldFilters_withLimitBelowMinimum_setsToMinimumPageLimit() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null, null, 2, false  // Below MIN_PAGE_LIMIT (5)
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(5, sanitized.getLimit());  // MIN_PAGE_LIMIT
  }

  @Test
  void testSanitizeFieldFilters_withLimitAboveMaximum_setsToMaximumPageLimit() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null, null, 150, false  // Above MAX_PAGE_LIMIT (100)
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(100, sanitized.getLimit());  // MAX_PAGE_LIMIT
  }

  @Test
  void testSanitizeFieldFilters_withValidLimit_preservesLimit() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null, null, 50, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(50, sanitized.getLimit());
  }

  @Test
  void testSanitizeFieldFilters_withNullLimit_setsToZero() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(0, sanitized.getLimit());
  }

  @Test
  void testSanitizeFieldFilters_withNegativeLimit_setsToZero() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null, null, -5, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(0, sanitized.getLimit());
  }

  @Test
  void testSanitizeFieldFilters_withValidPercentValues_preservesValues() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, 10, 90, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(10, sanitized.getPercentLinksInOperationFrom());
    assertEquals(90, sanitized.getPercentLinksInOperationTo());
  }

  @Test
  void testSanitizeFieldFilters_withComplexXSSPatterns_escapesAllSpecialCharacters() {
    // Given
    SortedSet<String> datasetName = new TreeSet<>(Set.of(
        "test&more<dangerous>\"quoted\"'single'",
        "normal_dataset_name"
    ));
    FieldFilters filters = new FieldFilters(
        null, null, null, datasetName, null,
        null, null, null, null, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertNotNull(sanitized.getDatasetName());
    assertEquals(2, sanitized.getDatasetName().size());
    assertTrue(sanitized.getDatasetName().contains(
        "test&amp;more&lt;dangerous&gt;&quot;quoted&quot;&#39;single&#39;"
    ));
    assertTrue(sanitized.getDatasetName().contains("normal_dataset_name"));
  }

  @Test
  void testSanitizeFieldFilters_withEmptyStringSet_preservesEmpty() {
    // Given
    SortedSet<String> provider = new TreeSet<>();
    FieldFilters filters = new FieldFilters(
        provider, null, null, null, null,
        null, null, null, null, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertNotNull(sanitized.getProvider());
    assertTrue(sanitized.getProvider().isEmpty());
  }

  @Test
  void testSanitizeFieldFilters_preservesDateFilters_noSanitization() {
    // Given
    LocalDate dateFrom = LocalDate.now();
    LocalDate dateTo = LocalDate.now().plus(7, ChronoUnit.DAYS);
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        dateFrom, dateTo, null, null, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(dateFrom, sanitized.getDateFrom());
    assertEquals(dateTo, sanitized.getDateTo());
  }

  @Test
  void testSanitizeFieldFilters_preservesExcludedCheckIds_noSanitization() {
    // Given
    SortedSet<String> excludedCheckId = new TreeSet<>(Set.of("dataset1","dataset2","dataset3"));
    FieldFilters filters = new FieldFilters(
        null, null, null, null, excludedCheckId,
        null, null, null, null, null, null, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(excludedCheckId, sanitized.getExcludedDatasetId());
  }

  @Test
  void testNoArgsConstructor_createsEmptyInstance() {
    // Given
    // No constructor arguments

    // When
    FieldFilters fieldFilters = new FieldFilters();

    // Then
    assertNotNull(fieldFilters);
    assertNull(fieldFilters.getProvider());
    assertNull(fieldFilters.getDataProvider());
    assertNull(fieldFilters.getDatasetId());
    assertNull(fieldFilters.getDatasetName());
    assertNull(fieldFilters.getExcludedDatasetId());
    assertNull(fieldFilters.getDateFrom());
    assertNull(fieldFilters.getDateTo());
    assertNull(fieldFilters.getPercentLinksInOperationFrom());
    assertNull(fieldFilters.getPercentLinksInOperationTo());
    assertNull(fieldFilters.getOffset());
    assertNull(fieldFilters.getLimit());
  }

  @Test
  void testSettersAndGetters_modifyAndRetrieveValues() {
    // Given
    FieldFilters fieldFilters = new FieldFilters();
    SortedSet<String> provider = new TreeSet<>(Set.of("provider1"));
    Integer limit = 25;

    // When
    fieldFilters.setProvider(provider);
    fieldFilters.setLimit(limit);

    // Then
    assertEquals(provider, fieldFilters.getProvider());
    assertEquals(limit, fieldFilters.getLimit());
  }

  @Test
  void testConstructor_sanitizesNumberInputs_convertingNegativeToZero() {
    // Given
    FieldFilters filters = new FieldFilters(
        null, null, null, null, null,
        null, null, null, null, -1, -1, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertEquals(0, sanitized.getOffset());
    assertEquals(0, sanitized.getLimit());
  }

  @Test
  void testSanitizeFieldFilters_withAllFieldsPopulated_preservesValidData() {
    // Given
    SortedSet<String> provider = new TreeSet<>(Set.of("provider1"));
    SortedSet<String> dataProvider = new TreeSet<>(Set.of("dataProvider1"));
    SortedSet<String> datasetId = new TreeSet<>(Set.of("dataset1"));
    SortedSet<String> datasetName = new TreeSet<>(Set.of("name1"));
    SortedSet<String> excludedCheckId = new TreeSet<>(Set.of("dataset1"));
    LocalDate dateFrom = LocalDate.now(ZoneOffset.UTC);
    LocalDate dateTo = LocalDate.from(Instant.now().plus(1, ChronoUnit.DAYS).atZone(ZoneOffset.UTC));
    Integer offset = 0;
    Integer limit = 50;

    FieldFilters filters = new FieldFilters(
        provider, dataProvider, datasetId, datasetName, excludedCheckId,
        dateFrom, dateTo, 20, 80, offset, limit, false
    );

    // When
    FieldFilters sanitized = FieldFilters.sanitizeFieldFilters(filters);

    // Then
    assertNotNull(sanitized);
    assertNotNull(sanitized.getProvider());
    assertNotNull(sanitized.getDataProvider());
    assertNotNull(sanitized.getDatasetId());
    assertNotNull(sanitized.getDatasetName());
    assertNotNull(sanitized.getExcludedDatasetId());
    assertNotNull(sanitized.getDateFrom());
    assertNotNull(sanitized.getDateTo());
    assertEquals(20, sanitized.getPercentLinksInOperationFrom());
    assertEquals(80, sanitized.getPercentLinksInOperationTo());
    assertEquals(0, sanitized.getOffset());
    assertEquals(50, sanitized.getLimit());
  }
}
