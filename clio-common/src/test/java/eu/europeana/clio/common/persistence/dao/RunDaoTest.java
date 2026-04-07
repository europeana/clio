package eu.europeana.clio.common.persistence.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.model.CheckRun;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
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
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunDaoTest {

  @Test
  void constructor_initializesHibernateSessionUtils() {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    // When
    RunDao runDao = new RunDao(sessionFactory);
    // Then
    assertNotNull(runDao);
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

    jakarta.persistence.criteria.Path<Long> path = mock(Path.class);
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

    jakarta.persistence.criteria.Path<Long> path = mock(Path.class);
    doReturn(path).when(run).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.lessThan(path, paramExpression)).thenReturn(predicate);

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
    jakarta.persistence.criteria.Path<Long> path = mock(Path.class);
    doReturn(path).when(run).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.greaterThanOrEqualTo(path, fromParamExpression)).thenReturn(fromPredicate);
    when(criteriaBuilder.lessThan(path, toParamExpression)).thenReturn(toPredicate);

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
    when(criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_CHECK_ID)).thenReturn(paramExpression);

    jakarta.persistence.criteria.Path<?> path = mock(Path.class);
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
    jakarta.persistence.criteria.Path<Object> path = mock(Path.class);
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
    CriteriaQuery<CheckRun> criteriaQuery = mock(CriteriaQuery.class);
    FieldFilters filters = new FieldFilters();
    filters = FieldFilters.sanitizeFieldFilters(filters);
    when(criteriaBuilder.createQuery(CheckRun.class)).thenReturn(criteriaQuery);

    Root<LinkRow> link = mock(Root.class);
    when(criteriaQuery.from(LinkRow.class)).thenReturn(link);

    Join<LinkRow, RunRow> run = mock(Join.class);
    doReturn(run).when(link).join("run", JoinType.INNER);

    Join<RunRow, DatasetRow> dataset = mock(Join.class);
    doReturn(dataset).when(run).join("dataset", JoinType.INNER);

    Join<RunRow, BatchRow> batch = mock(Join.class);
    doReturn(batch).when(run).join("batch", JoinType.INNER);

    // Mock the path methods for aggregations
    Path<Object> linkErrorPath = mock(Path.class);
    when(link.get(FieldNames.ERROR_MESSAGE_DB)).thenReturn(linkErrorPath);
    Expression<Long> countLinkErrors = mock(Expression.class);
    when(criteriaBuilder.count(linkErrorPath)).thenReturn(countLinkErrors);

    Expression<Long> countLink = mock(Expression.class);
    when(criteriaBuilder.count(link)).thenReturn(countLink);

    // Mock coalesce for errorsLinks: coalesce(count(errorMessage), 0).as(Long.class)
    Expression<?> coalescedErrorsTemp = mock(Expression.class);
    doReturn(coalescedErrorsTemp).when(criteriaBuilder).coalesce(countLinkErrors, 0);
    Expression<Long> coalescedErrors = mock(Expression.class);
    when(coalescedErrorsTemp.as(Long.class)).thenReturn(coalescedErrors);

    // Mock coalesce for totalLinks: coalesce(count(link), 0).as(Long.class)
    Expression<?> coalescedTotalTemp = mock(Expression.class);
    doReturn(coalescedTotalTemp).when(criteriaBuilder).coalesce(countLink, 0);
    Expression<Long> coalescedTotal = mock(Expression.class);
    when(coalescedTotalTemp.as(Long.class)).thenReturn(coalescedTotal);

    // Mock selectCase for percentLinksInOperation
    CriteriaBuilder.Case<Double> selectCaseWhen = mock(CriteriaBuilder.Case.class);
    doReturn(selectCaseWhen).when(criteriaBuilder).selectCase();

    Expression<Double> doubleErrors = mock(Expression.class);
    when(criteriaBuilder.toDouble(coalescedErrors)).thenReturn(doubleErrors);
    Expression<Double> doubleTotal = mock(Expression.class);
    when(criteriaBuilder.toDouble(coalescedTotal)).thenReturn(doubleTotal);

    Expression<Double> quotResult = mock(Expression.class);
    doReturn(quotResult).when(criteriaBuilder).quot(doubleErrors, doubleTotal);

    Expression<Double> quotResultCasted = mock(Expression.class);
    when(quotResult.as(Double.class)).thenReturn(quotResultCasted);

    Predicate equalExpr = mock(Predicate.class);
    doReturn(equalExpr).when(criteriaBuilder).equal(coalescedTotal, 0D);

    CriteriaBuilder.Case<Double> caseWhenThen = mock(CriteriaBuilder.Case.class);
    doReturn(caseWhenThen).when(selectCaseWhen).when(equalExpr, 0D);

    Expression<Double> caseResult = mock(Expression.class);
    doReturn(caseResult).when(caseWhenThen).otherwise(quotResultCasted);

    Expression<Double> prodResult = mock(Expression.class);
    doReturn(prodResult).when(criteriaBuilder).prod(caseResult, RunDao.HUNDRED);

    Expression<Double> diffResult = mock(Expression.class);
    doReturn(diffResult).when(criteriaBuilder).diff(RunDao.HUNDRED, prodResult);

    Expression<Integer> percentExpr = mock(Expression.class);
    when(diffResult.cast(Integer.class)).thenReturn(percentExpr);

    // When
    RunDao.CommonCheckRunsQueryParts<CheckRun> parts = RunDao.buildCommonCheckRunsQueryWithPredicates(criteriaBuilder,
        CheckRun.class, filters);

    // Then
    assertNotNull(parts);
    assertNotNull(parts.criteriaQuery());
    assertNotNull(parts.link());
    assertNotNull(parts.run());
    assertNotNull(parts.dataset());
    assertNotNull(parts.batch());
    assertNotNull(parts.wherePredicates());
    assertNotNull(parts.havingPredicates());
    assertNotNull(parts.parametersMap());
    assertNotNull(parts.errorsLinks());
    assertNotNull(parts.totalLinks());
    assertNotNull(parts.percentLinksInOperation());
  }

  @Test
  void buildCheckRunsQueryParts_hasEmptyPredicatesAndParameter() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<CheckRun> criteriaQuery = mock(CriteriaQuery.class);
    when(criteriaBuilder.createQuery(CheckRun.class)).thenReturn(criteriaQuery);
    FieldFilters filters = new FieldFilters();
    filters = FieldFilters.sanitizeFieldFilters(filters);
    Root<LinkRow> link = mock(Root.class);
    when(criteriaQuery.from(LinkRow.class)).thenReturn(link);

    Join<LinkRow, RunRow> run = mock(Join.class);
    doReturn(run).when(link).join("run", JoinType.INNER);

    Join<RunRow, DatasetRow> dataset = mock(Join.class);
    doReturn(dataset).when(run).join("dataset", JoinType.INNER);

    Join<RunRow, BatchRow> batch = mock(Join.class);
    doReturn(batch).when(run).join("batch", JoinType.INNER);

    // Mock the path methods for aggregations
    Path<Object> linkErrorPath = mock(Path.class);
    when(link.get(FieldNames.ERROR_MESSAGE_DB)).thenReturn(linkErrorPath);
    Expression<Long> countLinkErrors = mock(Expression.class);
    when(criteriaBuilder.count(linkErrorPath)).thenReturn(countLinkErrors);

    Expression<Long> countLink = mock(Expression.class);
    when(criteriaBuilder.count(link)).thenReturn(countLink);

    // Mock coalesce for errorsLinks: coalesce(count(errorMessage), 0).as(Long.class)
    Expression<?> coalescedErrorsTemp = mock(Expression.class);
    doReturn(coalescedErrorsTemp).when(criteriaBuilder).coalesce(countLinkErrors, 0);
    Expression<Long> coalescedErrors = mock(Expression.class);
    when(coalescedErrorsTemp.as(Long.class)).thenReturn(coalescedErrors);

    // Mock coalesce for totalLinks: coalesce(count(link), 0).as(Long.class)
    Expression<?> coalescedTotalTemp = mock(Expression.class);
    doReturn(coalescedTotalTemp).when(criteriaBuilder).coalesce(countLink, 0);
    Expression<Long> coalescedTotal = mock(Expression.class);
    when(coalescedTotalTemp.as(Long.class)).thenReturn(coalescedTotal);

    // Mock selectCase for percentLinksInOperation
    CriteriaBuilder.Case<Double> selectCaseWhen = mock(CriteriaBuilder.Case.class);
    doReturn(selectCaseWhen).when(criteriaBuilder).selectCase();

    Expression<Double> doubleErrors = mock(Expression.class);
    when(criteriaBuilder.toDouble(coalescedErrors)).thenReturn(doubleErrors);
    Expression<Double> doubleTotal = mock(Expression.class);
    when(criteriaBuilder.toDouble(coalescedTotal)).thenReturn(doubleTotal);

    Expression<Double> quotResult = mock(Expression.class);
    doReturn(quotResult).when(criteriaBuilder).quot(doubleErrors, doubleTotal);

    Expression<Double> quotResultCasted = mock(Expression.class);
    when(quotResult.as(Double.class)).thenReturn(quotResultCasted);

    Predicate equalExpr = mock(Predicate.class);
    doReturn(equalExpr).when(criteriaBuilder).equal(coalescedTotal, 0D);

    CriteriaBuilder.Case<Double> caseWhenThen = mock(CriteriaBuilder.Case.class);
    doReturn(caseWhenThen).when(selectCaseWhen).when(equalExpr, 0D);

    Expression<Double> caseResult = mock(Expression.class);
    doReturn(caseResult).when(caseWhenThen).otherwise(quotResultCasted);

    Expression<Double> prodResult = mock(Expression.class);
    doReturn(prodResult).when(criteriaBuilder).prod(caseResult, RunDao.HUNDRED);

    Expression<Double> diffResult = mock(Expression.class);
    doReturn(diffResult).when(criteriaBuilder).diff(RunDao.HUNDRED, prodResult);

    Expression<Integer> percentExpr = mock(Expression.class);
    when(diffResult.cast(Integer.class)).thenReturn(percentExpr);

    // When
    RunDao.CommonCheckRunsQueryParts<CheckRun> parts = RunDao.buildCommonCheckRunsQueryWithPredicates(criteriaBuilder,
        CheckRun.class, filters);

    // Then
    assertNotNull(parts);
    assertNotNull(parts.criteriaQuery());
    assertNotNull(parts.link());
    assertNotNull(parts.run());
    assertNotNull(parts.dataset());
    assertNotNull(parts.batch());
    assertNotNull(parts.wherePredicates());
    assertNotNull(parts.havingPredicates());
    assertNotNull(parts.parametersMap());
    assertNotNull(parts.errorsLinks());
    assertNotNull(parts.totalLinks());
    assertNotNull(parts.percentLinksInOperation());
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
      var mockDataset = mock(Dataset.class);
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
      var mockDataset = mock(Dataset.class);
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
