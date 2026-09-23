package eu.europeana.clio.common.persistence.dao;

import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.HUNDRED;
import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.buildCommonDatasetQueryWithPredicates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.model.DatasetCheckSummary;
import eu.europeana.clio.common.model.DatasetSummary;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.CommonDatasetQueryParts;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class DatasetDaoSupportTest {

  @Test
  void addPredicateAndParameterDateRange_addsDateFromPredicate() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Join<DatasetRow, RunRow> dataset = mock(Join.class);

    FieldFilters filters = new FieldFilters();
    LocalDate testDate = Instant.ofEpochMilli(1000000L).atZone(ZoneOffset.UTC).toLocalDate();
    filters.setDateFrom(testDate);

    ParameterExpression<Long> paramExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.STARTING_TIME_DB)).thenReturn(paramExpression);
    Predicate predicate = mock(Predicate.class);

    Path<Long> path = mock(Path.class);
    doReturn(path).when(dataset).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.greaterThanOrEqualTo(path, paramExpression)).thenReturn(predicate);

    // When
    DatasetDaoSupport.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, dataset, parametersMap);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);

    FieldFilters filters = new FieldFilters();
    LocalDate testDate = Instant.ofEpochMilli(1000000L).atZone(ZoneOffset.UTC).toLocalDate();
    filters.setDateTo(testDate);

    ParameterExpression<Long> paramExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.ENDING_TIME_DB)).thenReturn(paramExpression);
    Predicate predicate = mock(Predicate.class);

    Path<Long> path = mock(Path.class);
    doReturn(path).when(dataset).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.lessThan(path, paramExpression)).thenReturn(predicate);

    // When
    DatasetDaoSupport.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, dataset, parametersMap);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);

    FieldFilters filters = new FieldFilters();
    LocalDate fromDate = Instant.ofEpochMilli(1000000L).atZone(ZoneOffset.UTC).toLocalDate();
    LocalDate toDate = Instant.ofEpochMilli(2000000L).atZone(ZoneOffset.UTC).toLocalDate();
    filters.setDateFrom(fromDate);
    filters.setDateTo(toDate);

    ParameterExpression<Long> fromParamExpression = mock(ParameterExpression.class);
    ParameterExpression<Long> toParamExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.STARTING_TIME_DB)).thenReturn(fromParamExpression);
    when(criteriaBuilder.parameter(Long.class, FieldNames.ENDING_TIME_DB)).thenReturn(toParamExpression);

    Predicate fromPredicate = mock(Predicate.class);
    Predicate toPredicate = mock(Predicate.class);
    Path<Long> path = mock(Path.class);
    doReturn(path).when(dataset).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.greaterThanOrEqualTo(path, fromParamExpression)).thenReturn(fromPredicate);
    when(criteriaBuilder.lessThan(path, toParamExpression)).thenReturn(toPredicate);

    // When
    DatasetDaoSupport.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, dataset, parametersMap);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);

    FieldFilters filters = new FieldFilters();

    // When
    DatasetDaoSupport.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, dataset, parametersMap);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);
    Set<String> excludedIds = Set.of("datasetId1", "datasetId2", "datasetId3");

    ParameterExpression<Set> paramExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_ID)).thenReturn(paramExpression);

    Path<?> path = mock(Path.class);
    doReturn(path).when(dataset).get(FieldNames.DATASET_ID_DB);

    Predicate inPredicate = mock(Predicate.class);
    when(path.in(paramExpression)).thenReturn(inPredicate);

    Predicate notPredicate = mock(Predicate.class);
    when(criteriaBuilder.not(inPredicate)).thenReturn(notPredicate);

    // When
    DatasetDaoSupport.addPredicateAndParameterExcludedIds(excludedIds, criteriaBuilder, predicates, dataset, parametersMap);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);

    // When
    DatasetDaoSupport.addPredicateAndParameterExcludedIds(null, criteriaBuilder, predicates, dataset, parametersMap);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);

    // When
    DatasetDaoSupport.addPredicateAndParameterExcludedIds(Set.of(), criteriaBuilder, predicates, dataset, parametersMap);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);
    Set<String> fieldValue = Set.of("value1", "value2");
    String fieldName = "testField";

    ParameterExpression<Set> paramExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Set.class, fieldName + "Parameter")).thenReturn(paramExpression);
    Predicate predicate = mock(Predicate.class);
    Path<Object> path = mock(Path.class);
    when(dataset.get(fieldName)).thenReturn(path);
    when(path.in(paramExpression)).thenReturn(predicate);

    // When
    DatasetDaoSupport.addPredicateAndParameter(fieldValue, criteriaBuilder, predicates, dataset, parametersMap, fieldName);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);
    String fieldName = "testField";

    // When
    DatasetDaoSupport.addPredicateAndParameter(null, criteriaBuilder, predicates, dataset, parametersMap, fieldName);

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
    Join<DatasetRow, RunRow> dataset = mock(Join.class);
    String fieldName = "testField";

    // When
    DatasetDaoSupport.addPredicateAndParameter(Set.of(), criteriaBuilder, predicates, dataset, parametersMap, fieldName);

    // Then
    assertTrue(predicates.isEmpty());
    assertTrue(parametersMap.isEmpty());
  }

  @Test
  void addPredicateAndParameterLastThreeMonths() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    Root<LinkRow> link = mock(Root.class);

    ParameterExpression<Long> fromParamExpression = mock(ParameterExpression.class);
    ParameterExpression<Long> toParamExpression = mock(ParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.STARTING_WINDOW_TIME_DB)).thenReturn(fromParamExpression);
    when(criteriaBuilder.parameter(Long.class, FieldNames.ENDING_WINDOW_TIME_DB)).thenReturn(toParamExpression);

    Predicate fromPredicate = mock(Predicate.class);
    Predicate toPredicate = mock(Predicate.class);
    Path<Long> pathS = mock(Path.class);
    doReturn(pathS).when(link).get(FieldNames.STARTING_WINDOW_TIME_DB);
    when(criteriaBuilder.greaterThanOrEqualTo(pathS, fromParamExpression)).thenReturn(fromPredicate);
    Path<Long> pathE = mock(Path.class);
    doReturn(pathE).when(link).get(FieldNames.ENDING_WINDOW_TIME_DB);
    when(criteriaBuilder.lessThan(pathE, toParamExpression)).thenReturn(toPredicate);

    // When
    DatasetDaoSupport.addPredicateAndParameterLastThreeMonths(criteriaBuilder, predicates, link, parametersMap);

    // Then
    assertEquals(2, predicates.size());
    assertEquals(2, parametersMap.size());
  }

  @Test
  void buildRunSummaryQueryParts_buildsAllPartsSuccessfully() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<DatasetSummary> criteriaQuery = mock(CriteriaQuery.class);
    FieldFilters filters = new FieldFilters();
    filters = FieldFilters.sanitizeFieldFilters(filters);
    when(criteriaBuilder.createQuery(DatasetSummary.class)).thenReturn(criteriaQuery);

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
    doReturn(prodResult).when(criteriaBuilder).prod(caseResult, HUNDRED);

    Expression<Double> diffResult = mock(Expression.class);
    doReturn(diffResult).when(criteriaBuilder).diff(HUNDRED, prodResult);

    Expression<Integer> percentExpr = mock(Expression.class);
    when(diffResult.cast(Integer.class)).thenReturn(percentExpr);

    // When
    CommonDatasetQueryParts<DatasetSummary> parts = buildCommonDatasetQueryWithPredicates(criteriaBuilder,
        DatasetSummary.class, filters);

    // Then
    assertNotNull(parts);
    assertNotNull(parts.criteriaQuery());
    assertNotNull(parts.link());
    assertNotNull(parts.run());
    assertNotNull(parts.dataset());
    assertNotNull(parts.wherePredicates());
    assertNotNull(parts.havingPredicates());
    assertNotNull(parts.parametersMap());
    assertNotNull(parts.errorsLinks());
    assertNotNull(parts.totalLinks());
    assertNotNull(parts.percentLinksInOperation());
  }

  @Test
  void buildDatasetSummaryQueryParts_hasEmptyPredicatesAndParameter() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<DatasetSummary> criteriaQuery = mock(CriteriaQuery.class);
    when(criteriaBuilder.createQuery(DatasetSummary.class)).thenReturn(criteriaQuery);
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
    doReturn(prodResult).when(criteriaBuilder).prod(caseResult, HUNDRED);

    Expression<Double> diffResult = mock(Expression.class);
    doReturn(diffResult).when(criteriaBuilder).diff(HUNDRED, prodResult);

    Expression<Integer> percentExpr = mock(Expression.class);
    when(diffResult.cast(Integer.class)).thenReturn(percentExpr);

    // When
    CommonDatasetQueryParts<DatasetSummary> parts = DatasetDaoSupport.buildCommonDatasetQueryWithPredicates(criteriaBuilder,
        DatasetSummary.class, filters);

    // Then
    assertNotNull(parts);
    assertNotNull(parts.criteriaQuery());
    assertNotNull(parts.link());
    assertNotNull(parts.run());
    assertNotNull(parts.dataset());
    assertNotNull(parts.wherePredicates());
    assertNotNull(parts.havingPredicates());
    assertNotNull(parts.parametersMap());
    assertNotNull(parts.errorsLinks());
    assertNotNull(parts.totalLinks());
    assertNotNull(parts.percentLinksInOperation());
  }

  @Test
  void buildDatasetCheckSummaryQueryParts_buildsAllPartsSuccessfully() {
    // Given
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<DatasetCheckSummary> criteriaQuery = mock(CriteriaQuery.class);
    FieldFilters filters = new FieldFilters();
    filters.setDatasetId(new TreeSet<>(Set.of("datasetId1")));
    filters = FieldFilters.sanitizeFieldFilters(filters);
    when(criteriaBuilder.createQuery(DatasetCheckSummary.class)).thenReturn(criteriaQuery);

    Root<LinkRow> link = mock(Root.class);
    when(criteriaQuery.from(LinkRow.class)).thenReturn(link);

    Join<LinkRow, RunRow> run = mock(Join.class);
    doReturn(run).when(link).join("run", JoinType.INNER);

    Path<Object> datasetPth = mock(Path.class);
    when(run.get("dataset")).thenReturn(datasetPth);
    Path<Object> datasetValue = mock(Path.class);
    when(datasetPth.get(FieldNames.DATASET_ID_DB)).thenReturn(datasetValue);

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
    doReturn(prodResult).when(criteriaBuilder).prod(caseResult, HUNDRED);

    Expression<Double> diffResult = mock(Expression.class);
    doReturn(diffResult).when(criteriaBuilder).diff(HUNDRED, prodResult);

    Expression<Integer> percentExpr = mock(Expression.class);
    when(diffResult.cast(Integer.class)).thenReturn(percentExpr);

    // When
    CommonDatasetQueryParts<DatasetCheckSummary> parts = DatasetDaoSupport.buildCommonDatasetChecksQueryWithPredicates(criteriaBuilder,
        DatasetCheckSummary.class, filters);

    // Then
    assertNotNull(parts);
    assertNotNull(parts.criteriaQuery());
    assertNotNull(parts.link());
    assertNotNull(parts.run());
    assertNull(parts.dataset());
    assertNotNull(parts.wherePredicates());
    assertNotNull(parts.havingPredicates());
    assertNotNull(parts.parametersMap());
    assertNotNull(parts.errorsLinks());
    assertNotNull(parts.totalLinks());
    assertNotNull(parts.percentLinksInOperation());
  }
}
