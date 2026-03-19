package eu.europeana.clio.common.persistence.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunDaoTest {

  @Mock
  private SessionFactory sessionFactory;

  @Test
  void constructor_initializesHibernateSessionUtils() {
    RunDao runDao = new RunDao(sessionFactory);
    assertNotNull(runDao);
  }

  @Test
  void percentLinksInOperation_returnsTrueWhenNullFilters() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationFrom(0);
    filters.setPercentLinksInOperationTo(50);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(50L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertTrue(result);
  }

  @Test
  void percentLinksInOperation_returnsTrueWhenWithinRange() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationFrom(40);
    filters.setPercentLinksInOperationTo(60);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(50L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertTrue(result);
  }

  @Test
  void percentLinksInOperation_returnsFalseWhenBelowMinimum() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationFrom(60);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(50L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertFalse(result);
  }

  @Test
  void percentLinksInOperation_returnsFalseWhenAboveMaximum() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationTo(40);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(50L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertFalse(result);
  }

  @Test
  void percentLinksInOperation_returnsTrueWhenAtMinimumBoundary() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationFrom(50);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(50L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertTrue(result);
  }

  @Test
  void percentLinksInOperation_returnsTrueWhenAtMaximumBoundary() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationTo(50);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(50L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertTrue(result);
  }

  @Test
  void percentLinksInOperation_returnsFalseWhenBelowMinimumAndAboveMaximum() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationFrom(60);
    filters.setPercentLinksInOperationTo(40);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(50L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertFalse(result);
  }

  @Test
  void percentLinksInOperation_returnsTrueOnlyFromFilterSet() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationFrom(30);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(75L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertTrue(result);
  }

  @Test
  void percentLinksInOperation_returnsTrueOnlyToFilterSet() {
    // Given
    FieldFilters filters = new FieldFilters();
    filters.setPercentLinksInOperationTo(80);
    Tuple tuple = mock(Tuple.class);
    when(tuple.get(FieldNames.ERROR_LINKS_DB)).thenReturn(75L);
    when(tuple.get(FieldNames.TOTAL_LINKS_DB)).thenReturn(100L);

    // When
    boolean result = RunDao.percentLinksInOperation(filters, tuple);

    // Then
    assertTrue(result);
  }

  @Test
  void addPredicateAndParameterDateRange_addsDateFromPredicate() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<LinkRow, RunRow> run = mock(Join.class);

    FieldFilters filters = new FieldFilters();
    Date testDate = new Date(1000000L);
    filters.setDateFrom(testDate);

    ParameterExpression<Long> paramExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.STARTING_TIME_DB)).thenReturn(paramExpression);
    Predicate predicate = mock(Predicate.class);

    jakarta.persistence.criteria.Path<Long> path = mock(jakarta.persistence.criteria.Path.class);
    doReturn(path).when(run).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.greaterThanOrEqualTo(path, paramExpression)).thenReturn(predicate);

    // When
    RunDao.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, run, parametersMap);

    // Then
    assertEquals(1, predicates.size());
    assertEquals(1, parametersMap.size());
  }

  @Test
  void addPredicateAndParameterDateRange_addsDateToPredicate() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<LinkRow, RunRow> run = mock(Join.class);

    FieldFilters filters = new FieldFilters();
    Date testDate = new Date(1000000L);
    filters.setDateTo(testDate);

    ParameterExpression<Long> paramExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.ENDING_TIME_DB)).thenReturn(paramExpression);
    Predicate predicate = mock(Predicate.class);

    jakarta.persistence.criteria.Path<Long> path = mock(jakarta.persistence.criteria.Path.class);
    doReturn(path).when(run).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.lessThanOrEqualTo(path, paramExpression)).thenReturn(predicate);

    // When
    RunDao.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, run, parametersMap);

    // Then
    assertEquals(1, predicates.size());
    assertEquals(1, parametersMap.size());
  }

  @Test
  void addPredicateAndParameterDateRange_addsBothDatePredicates() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<LinkRow, RunRow> run = mock(Join.class);

    FieldFilters filters = new FieldFilters();
    Date fromDate = new Date(1000000L);
    Date toDate = new Date(2000000L);
    filters.setDateFrom(fromDate);
    filters.setDateTo(toDate);

    ParameterExpression<Long> fromParamExpression = mock(ParameterExpression.class);
    ParameterExpression<Long> toParamExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.STARTING_TIME_DB)).thenReturn(fromParamExpression);
    when(criteriaBuilder.parameter(Long.class, FieldNames.ENDING_TIME_DB)).thenReturn(toParamExpression);

    Predicate fromPredicate = mock(Predicate.class);
    Predicate toPredicate = mock(Predicate.class);
    jakarta.persistence.criteria.Path<Long> path = mock(jakarta.persistence.criteria.Path.class);
    doReturn(path).when(run).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.greaterThanOrEqualTo(path, fromParamExpression)).thenReturn(fromPredicate);
    when(criteriaBuilder.lessThanOrEqualTo(path, toParamExpression)).thenReturn(toPredicate);

    // When
    RunDao.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, run, parametersMap);

    // Then
    assertEquals(2, predicates.size());
    assertEquals(2, parametersMap.size());
  }

  @Test
  void addPredicateAndParameterDateRange_noDateFilters() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<LinkRow, RunRow> run = mock(Join.class);

    FieldFilters filters = new FieldFilters();

    // When
    RunDao.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, run, parametersMap);

    // Then
    assertTrue(predicates.isEmpty());
    assertTrue(parametersMap.isEmpty());
  }

  @Test
  void addPredicateAndParameterExcludedIds_addsPredicateWhenExcludedIdsPresent() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<LinkRow, RunRow> run = mock(Join.class);
    Set<Long> excludedIds = Set.of(1L, 2L, 3L);

    ParameterExpression<Set> paramExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_CHECK_IDS)).thenReturn(paramExpression);

    jakarta.persistence.criteria.Path<?> path = mock(jakarta.persistence.criteria.Path.class);
    doReturn(path).when(run).get(FieldNames.RUN_ID_DB);

    Predicate inPredicate = mock(Predicate.class);
    when(path.in(paramExpression)).thenReturn(inPredicate);

    Predicate notPredicate = mock(Predicate.class);
    when(criteriaBuilder.not(inPredicate)).thenReturn(notPredicate);

    // When
    RunDao.addPredicateAndParameterExcludedIds(excludedIds, criteriaBuilder, predicates, run, parametersMap);

    // Then
    assertEquals(1, predicates.size());
    assertEquals(1, parametersMap.size());
  }

  @Test
  void addPredicateAndParameterExcludedIds_noPredicateWhenExcludedIdsNull() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<LinkRow, RunRow> run = mock(Join.class);

    // When
    RunDao.addPredicateAndParameterExcludedIds(null, criteriaBuilder, predicates, run, parametersMap);

    // Then
    assertTrue(predicates.isEmpty());
    assertTrue(parametersMap.isEmpty());
  }

  @Test
  void addPredicateAndParameterExcludedIds_noPredicateWhenExcludedIdsEmpty() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<LinkRow, RunRow> run = mock(Join.class);

    // When
    RunDao.addPredicateAndParameterExcludedIds(Set.of(), criteriaBuilder, predicates, run, parametersMap);

    // Then
    assertTrue(predicates.isEmpty());
    assertTrue(parametersMap.isEmpty());
  }

  @Test
  void addPredicateAndParameter_addsPredicateWhenFieldValuePresent() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<RunRow, DatasetRow> dataset = mock(Join.class);
    Set<String> fieldValue = Set.of("value1", "value2");
    String fieldName = "testField";

    ParameterExpression<Set> paramExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Set.class, fieldName + "Parameter")).thenReturn(paramExpression);
    Predicate predicate = mock(Predicate.class);
    jakarta.persistence.criteria.Path<Object> path = mock(jakarta.persistence.criteria.Path.class);
    when(dataset.get(fieldName)).thenReturn(path);
    when(path.in(paramExpression)).thenReturn(predicate);

    // When
    RunDao.addPredicateAndParameter(fieldValue, criteriaBuilder, predicates, dataset, parametersMap, fieldName);

    // Then
    assertEquals(1, predicates.size());
    assertEquals(1, parametersMap.size());
  }

  @Test
  void addPredicateAndParameter_noPredicateWhenFieldValueNull() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<RunRow, DatasetRow> dataset = mock(Join.class);
    String fieldName = "testField";

    // When
    RunDao.addPredicateAndParameter(null, criteriaBuilder, predicates, dataset, parametersMap, fieldName);

    // Then
    assertTrue(predicates.isEmpty());
    assertTrue(parametersMap.isEmpty());
  }

  @Test
  void addPredicateAndParameter_noPredicateWhenFieldValueEmpty() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<RunRow, DatasetRow> dataset = mock(Join.class);
    String fieldName = "testField";

    // When
    RunDao.addPredicateAndParameter(Set.of(), criteriaBuilder, predicates, dataset, parametersMap, fieldName);

    // Then
    assertTrue(predicates.isEmpty());
    assertTrue(parametersMap.isEmpty());
  }

  @Test
  void buildCheckRunsQueryParts_buildsAllPartsSuccessfully() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<Tuple> criteriaQuery = mock(CriteriaQuery.class);
    when(criteriaBuilder.createTupleQuery()).thenReturn(criteriaQuery);

    Root<LinkRow> link = mock(Root.class);
    doReturn(link).when(criteriaQuery).from(LinkRow.class);

    Join<LinkRow, RunRow> run = mock(Join.class);
    doReturn(run).when(link).join("run", JoinType.INNER);

    Join<RunRow, DatasetRow> dataset = mock(Join.class);
    doReturn(dataset).when(run).join("dataset", JoinType.INNER);

    Join<RunRow, BatchRow> batch = mock(Join.class);
    doReturn(batch).when(run).join("batch", JoinType.INNER);

    // When
    RunDao.QueryParts parts = RunDao.buildCheckRunsQueryParts(criteriaBuilder);

    // Then
    assertNotNull(parts);
    assertNotNull(parts.criteriaQuery());
    assertNotNull(parts.link());
    assertNotNull(parts.run());
    assertNotNull(parts.dataset());
    assertNotNull(parts.batch());
    assertNotNull(parts.predicates());
    assertNotNull(parts.parametersMap());
    assertTrue(parts.predicates().isEmpty());
    assertTrue(parts.parametersMap().isEmpty());
  }

  @Test
  void buildCheckRunsQueryParts_hasEmptyPredicatesAndParameters() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<Tuple> criteriaQuery = mock(CriteriaQuery.class);
    when(criteriaBuilder.createTupleQuery()).thenReturn(criteriaQuery);

    Root<LinkRow> link = mock(Root.class);
    doReturn(link).when(criteriaQuery).from(LinkRow.class);

    Join<LinkRow, RunRow> run = mock(Join.class);
    doReturn(run).when(link).join("run", JoinType.INNER);

    Join<RunRow, DatasetRow> dataset = mock(Join.class);
    doReturn(dataset).when(run).join("dataset", JoinType.INNER);

    Join<RunRow, BatchRow> batch = mock(Join.class);
    doReturn(batch).when(run).join("batch", JoinType.INNER);

    // When
    RunDao.QueryParts parts = RunDao.buildCheckRunsQueryParts(criteriaBuilder);

    // Then
    List<Predicate> predicates = parts.predicates();
    Map<ParameterExpression<?>, Object> parametersMap = parts.parametersMap();

    assertEquals(0, predicates.size());
    assertEquals(0, parametersMap.size());
  }

  @Test
  void convert_convertsRunRowToRun() {
    // Given
    RunRow runRow = mock(RunRow.class);
    long runId = 456L;
    Instant startingTime = Instant.now();
    DatasetRow datasetRow = mock(DatasetRow.class);

    when(runRow.getRunId()).thenReturn(runId);
    when(runRow.getStartingTime()).thenReturn(startingTime);
    when(runRow.getDataset()).thenReturn(datasetRow);

    try (MockedStatic<DatasetDao> datasetDaoMock = mockStatic(DatasetDao.class)) {
      var mockDataset = mock(eu.europeana.clio.common.model.Dataset.class);
      datasetDaoMock.when(() -> DatasetDao.convert(datasetRow)).thenReturn(mockDataset);

      // When
      Run result = RunDao.convert(runRow);

      // Then
      assertNotNull(result);
      assertEquals(runId, result.getRunId());
      assertEquals(startingTime, result.getStartingTime());
      assertEquals(mockDataset, result.getDataset());
    }
  }

  @Test
  void convert_preservesAllRunAttributes() {
    // Given
    RunRow runRow = mock(RunRow.class);
    long runId = 789L;
    Instant startingTime = Instant.ofEpochMilli(1234567890L);
    DatasetRow datasetRow = mock(DatasetRow.class);

    when(runRow.getRunId()).thenReturn(runId);
    when(runRow.getStartingTime()).thenReturn(startingTime);
    when(runRow.getDataset()).thenReturn(datasetRow);

    try (MockedStatic<DatasetDao> datasetDaoMock = mockStatic(DatasetDao.class)) {
      var mockDataset = mock(eu.europeana.clio.common.model.Dataset.class);
      datasetDaoMock.when(() -> DatasetDao.convert(datasetRow)).thenReturn(mockDataset);

      // When
      Run result = RunDao.convert(runRow);

      // Then
      assertEquals(789L, result.getRunId());
      assertEquals(Instant.ofEpochMilli(1234567890L), result.getStartingTime());
      assertEquals(mockDataset, result.getDataset());
    }
  }
}
