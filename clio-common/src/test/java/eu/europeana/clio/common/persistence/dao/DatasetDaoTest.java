package eu.europeana.clio.common.persistence.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.model.DatasetSummary;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.persistence.dao.DatasetDao.CommonDatasetQueryParts;
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
import java.util.stream.Stream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.junit.jupiter.api.Test;

class DatasetDaoTest {

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
    DatasetDao.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, dataset, parametersMap);

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
    DatasetDao.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, dataset, parametersMap);

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
    DatasetDao.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, dataset, parametersMap);

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
    DatasetDao.addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, dataset, parametersMap);

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
    when(criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_DATASET_ID)).thenReturn(paramExpression);

    Path<?> path = mock(Path.class);
    doReturn(path).when(dataset).get(FieldNames.DATASET_ID_DB);

    Predicate inPredicate = mock(Predicate.class);
    when(path.in(paramExpression)).thenReturn(inPredicate);

    Predicate notPredicate = mock(Predicate.class);
    when(criteriaBuilder.not(inPredicate)).thenReturn(notPredicate);

    // When
    DatasetDao.addPredicateAndParameterExcludedIds(excludedIds, criteriaBuilder, predicates, dataset, parametersMap);

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
    DatasetDao.addPredicateAndParameterExcludedIds(null, criteriaBuilder, predicates, dataset, parametersMap);

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
    DatasetDao.addPredicateAndParameterExcludedIds(Set.of(), criteriaBuilder, predicates, dataset, parametersMap);

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
    DatasetDao.addPredicateAndParameter(fieldValue, criteriaBuilder, predicates, dataset, parametersMap, fieldName);

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
    DatasetDao.addPredicateAndParameter(null, criteriaBuilder, predicates, dataset, parametersMap, fieldName);

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
    DatasetDao.addPredicateAndParameter(Set.of(), criteriaBuilder, predicates, dataset, parametersMap, fieldName);

    // Then
    assertTrue(predicates.isEmpty());
    assertTrue(parametersMap.isEmpty());
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
    doReturn(prodResult).when(criteriaBuilder).prod(caseResult, DatasetDao.HUNDRED);

    Expression<Double> diffResult = mock(Expression.class);
    doReturn(diffResult).when(criteriaBuilder).diff(DatasetDao.HUNDRED, prodResult);

    Expression<Integer> percentExpr = mock(Expression.class);
    when(diffResult.cast(Integer.class)).thenReturn(percentExpr);

    // When
    CommonDatasetQueryParts<DatasetSummary> parts = DatasetDao.buildCommonDatasetQueryWithPredicates(criteriaBuilder,
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
  void buildRunSummaryQueryParts_hasEmptyPredicatesAndParameter() {
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
    doReturn(prodResult).when(criteriaBuilder).prod(caseResult, DatasetDao.HUNDRED);

    Expression<Double> diffResult = mock(Expression.class);
    doReturn(diffResult).when(criteriaBuilder).diff(DatasetDao.HUNDRED, prodResult);

    Expression<Integer> percentExpr = mock(Expression.class);
    when(diffResult.cast(Integer.class)).thenReturn(percentExpr);

    // When
    CommonDatasetQueryParts<DatasetSummary> parts = DatasetDao.buildCommonDatasetQueryWithPredicates(criteriaBuilder,
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


  private Query mockQuery(SessionFactory sessionFactory,
      Session session,
      HibernateCriteriaBuilder criteriaBuilder,
      JpaJoin dataset) {
    when(sessionFactory.openSession()).thenReturn(session);
    JpaCriteriaQuery<DatasetSummary> criteriaQuery = mock(JpaCriteriaQuery.class);

    when(session.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createQuery(DatasetSummary.class)).thenReturn(criteriaQuery);
    Query query = mock(Query.class);
    when(query.getResultStream()).thenReturn(Stream.empty());
    when(session.createQuery(any(CriteriaQuery.class))).thenReturn(query);
    JpaRoot<LinkRow> link = mock(JpaRoot.class);
    when(criteriaQuery.from(LinkRow.class)).thenReturn(link);
    Join<RunRow, DatasetRow> run = mock(JpaJoin.class);
    doReturn(run).when(link).join("run", JoinType.INNER);

    doReturn(dataset).when(run).join("dataset", JoinType.INNER);

    // Mock the path methods for aggregations
    JpaPath<Object> linkErrorPath = mock(JpaPath.class);
    when(link.get(FieldNames.ERROR_MESSAGE_DB)).thenReturn(linkErrorPath);
    JpaExpression<Long> countLinkErrors = mock(JpaExpression.class);
    when(criteriaBuilder.count(linkErrorPath)).thenReturn(countLinkErrors);

    JpaExpression<Long> countLink = mock(JpaExpression.class);
    when(criteriaBuilder.count(link)).thenReturn(countLink);

    // Mock coalesce for errorsLinks: coalesce(count(errorMessage), 0).as(Long.class)
    JpaCoalesce coalescedErrorsTemp = mock(JpaCoalesce.class);
    when(criteriaBuilder.coalesce(countLinkErrors, 0)).thenReturn(coalescedErrorsTemp);
    JpaExpression<Long> coalescedErrors = mock(JpaExpression.class);
    when(coalescedErrorsTemp.as(Long.class)).thenReturn(coalescedErrors);

    // Mock coalesce for totalLinks: coalesce(count(link), 0).as(Long.class)
    JpaCoalesce coalescedTotalTemp = mock(JpaCoalesce.class);
    when(criteriaBuilder.coalesce(countLink, 0)).thenReturn(coalescedTotalTemp);
    JpaExpression<Long> coalescedTotal = mock(JpaExpression.class);
    when(coalescedTotalTemp.as(Long.class)).thenReturn(coalescedTotal);

    // Mock selectCase for percentLinksInOperation
    JpaSearchedCase<Double> selectCaseWhen = mock(JpaSearchedCase.class);
    doReturn(selectCaseWhen).when(criteriaBuilder).selectCase();

    JpaExpression<Double> doubleErrors = mock(JpaExpression.class);
    when(criteriaBuilder.toDouble(coalescedErrors)).thenReturn(doubleErrors);

    JpaExpression<Double> doubleTotal = mock(JpaExpression.class);
    when(criteriaBuilder.toDouble(coalescedTotal)).thenReturn(doubleTotal);

    JpaExpression<Number> longStartingTime = mock(JpaExpression.class);
    when(criteriaBuilder.min(any())).thenReturn(longStartingTime);

    JpaExpression<Double> quotResult = mock(JpaExpression.class);
    doReturn(quotResult).when(criteriaBuilder).quot(doubleErrors, doubleTotal);

    JpaExpression<Double> quotResultCasted = mock(JpaExpression.class);
    when(quotResult.as(Double.class)).thenReturn(quotResultCasted);

    JpaPredicate equalExpr = mock(JpaPredicate.class);
    when(criteriaBuilder.equal(coalescedTotal, 0D)).thenReturn(equalExpr);

    JpaSearchedCase<Double> caseWhenThen = mock(JpaSearchedCase.class);
    when(selectCaseWhen.when(equalExpr, 0D)).thenReturn(caseWhenThen);

    JpaExpression<Double> caseResult = mock(JpaExpression.class);
    doReturn(caseResult).when(caseWhenThen).otherwise(quotResultCasted);

    JpaExpression<Double> prodResult = mock(JpaExpression.class);
    doReturn(prodResult).when(criteriaBuilder).prod(caseResult, DatasetDao.HUNDRED);

    JpaExpression<Double> diffResult = mock(JpaExpression.class);
    doReturn(diffResult).when(criteriaBuilder).diff(DatasetDao.HUNDRED, prodResult);

    JpaExpression<Integer> percentExpr = mock(JpaExpression.class);
    when(diffResult.cast(Integer.class)).thenReturn(percentExpr);

    return query;
  }

  @Test
  void findDatasetsSummaryFilterOptions_withEmptyResults_returnsFilterWithEmptySets() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    when(query.getResultStream()).thenReturn(Stream.empty());

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertTrue(result.getProvider() == null || result.getProvider().isEmpty());
    assertTrue(result.getDataProvider() == null || result.getDataProvider().isEmpty());
  }

  @Test
  void findDatasetsSummaryFilterOptions_withSingleRunSummary_returnsSingleFilterOption() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(1);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary datasetSummary = new DatasetSummary("dataset1", "Dataset 1", 100L, LocalDate.now(),
        "provider1", "dataProvider1", 75
    );
    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertTrue(result.getProvider().contains("provider1"));
    assertTrue(result.getDataProvider().contains("dataProvider1"));
    assertTrue(result.getDatasetId().contains("dataset1"));
    assertTrue(result.getDatasetName().contains("Dataset 1"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_withMultipleRunSummaries_returnsMultipleFilterOptions() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(2);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary datasetSummary1 = new DatasetSummary("dataset1", "Dataset 1", 100L, LocalDate.now(),
        "provider1", "dataProvider1", 75
    );
    DatasetSummary datasetSummary2 = new DatasetSummary("dataset2", "Dataset 2", 200L, LocalDate.now(),
        "provider2", "dataProvider2", 80
    );
    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary1, datasetSummary2));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getProvider().size());
    assertTrue(result.getProvider().contains("provider1"));
    assertTrue(result.getProvider().contains("provider2"));
    assertEquals(2, result.getDatasetId().size());
    assertTrue(result.getDatasetId().contains("dataset1"));
    assertTrue(result.getDatasetId().contains("dataset2"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_filtersOutNullValues() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(2);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary datasetSummary1 = new DatasetSummary("dataset1", "Dataset 1", 100L, LocalDate.now(),
        "provider1", "dataProvider1", 75
    );
    DatasetSummary datasetSummary2 = new DatasetSummary("dataset2", "Dataset 2", 200L, LocalDate.now(), null, "dataProvider2",
        80);
    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary1, datasetSummary2));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getDatasetId().size());
    assertTrue(result.getDatasetId().contains("dataset1"));
    assertEquals(1, result.getProvider().size());
    assertTrue(result.getProvider().contains("provider1"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_filtersOutEmptyStrings() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(2);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary datasetSummary1 = new DatasetSummary("dataset1", "Dataset 1", 100L, LocalDate.now(),
        "provider1", "dataProvider1", 75
    );
    DatasetSummary datasetSummary2 = new DatasetSummary("", "", 200L, LocalDate.now(),
        "", "", 80
    );
    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary1, datasetSummary2));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getDatasetName().size());
    assertTrue(result.getDatasetName().contains("Dataset 1"));
    assertEquals(1, result.getDataProvider().size());
    assertTrue(result.getDataProvider().contains("dataProvider1"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_extractsProviderFieldCorrectly() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(3);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary summary1 = new DatasetSummary("ds1", "Dataset 1", 100L, LocalDate.now(),
        "provider1", "dp1", 75);
    DatasetSummary summary2 = new DatasetSummary("ds2", "Dataset 2", 200L, LocalDate.now(),
        "provider2", "dp2", 80);
    DatasetSummary summary3 = new DatasetSummary("ds3", "Dataset 3", 300L, LocalDate.now(),
        "provider1", "dp3", 85);
    when(query.getResultStream()).thenReturn(Stream.of(summary1, summary2, summary3));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getProvider().size());
    assertTrue(result.getProvider().contains("provider1"));
    assertTrue(result.getProvider().contains("provider2"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_extractsDataProviderFieldCorrectly() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(3);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary summary1 = new DatasetSummary("ds1", "Dataset 1", 100L, LocalDate.now(),
        "prov1", "dataProvider1", 75);
    DatasetSummary summary2 = new DatasetSummary("ds2", "Dataset 2", 200L, LocalDate.now(),
        "prov2", "dataProvider2", 80);
    DatasetSummary summary3 = new DatasetSummary("ds3", "Dataset 3", 300L, LocalDate.now(),
        "prov3", "dataProvider1", 85);
    when(query.getResultStream()).thenReturn(Stream.of(summary1, summary2, summary3));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getDataProvider().size());
    assertTrue(result.getDataProvider().contains("dataProvider1"));
    assertTrue(result.getDataProvider().contains("dataProvider2"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_extractsDatasetIdFieldCorrectly() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(3);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary summary1 = new DatasetSummary("datasetId1", "Dataset 1", 100L, LocalDate.now(),
        "prov1", "dp1", 75);
    DatasetSummary summary2 = new DatasetSummary("datasetId2", "Dataset 2", 200L, LocalDate.now(),
        "prov2", "dp2", 80);
    DatasetSummary summary3 = new DatasetSummary("datasetId1", "Dataset 1 Copy", 300L, LocalDate.now(),
        "prov1", "dp1", 85);

    when(query.getResultStream()).thenReturn(Stream.of(summary1, summary2, summary3));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getDatasetId().size());
    assertTrue(result.getDatasetId().contains("datasetId1"));
    assertTrue(result.getDatasetId().contains("datasetId2"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_extractsDatasetNameFieldCorrectly() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(3);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary summary1 = new DatasetSummary("ds1", "DatasetName1", 100L, LocalDate.now(),
        "prov1", "dp1", 75);
    DatasetSummary summary2 = new DatasetSummary("ds2", "DatasetName2", 200L, LocalDate.now(),
        "prov2", "dp2", 80);
    DatasetSummary summary3 = new DatasetSummary("ds3", "DatasetName1", 300L, LocalDate.now(),
        "prov3", "dp3", 85);

    when(query.getResultStream()).thenReturn(Stream.of(summary1, summary2, summary3));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getDatasetName().size());
    assertTrue(result.getDatasetName().contains("DatasetName1"));
    assertTrue(result.getDatasetName().contains("DatasetName2"));
  }

  //  @Test
  //  void findDatasetsSummaryFilterOptions_preservesExcludedCheckId() throws Exception {
  //    // Given
  //    SessionFactory sessionFactory = mock(SessionFactory.class);
  //    DatasetDao datasetDao = new DatasetDao(sessionFactory);
  //    FieldFilters inputFilters = new FieldFilters();
  //    SortedSet<Long> excludedCheckIds = new TreeSet<>(Set.of(1L, 2L, 3L));
  //    inputFilters.setExcludedCheckId(excludedCheckIds);
  //    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
  //
  //    Session session = mock(Session.class);
  //    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
  //    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
  //    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);
  //
  //    DatasetSummary datasetSummary = new DatasetSummary("dataset", "Dataset", 100L, LocalDate.now(),
  //        "provider", "dataProvider", 75);
  //
  //    JpaParameterExpression<Set> paramExpression = mock(JpaParameterExpression.class);
  //    when(criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_CHECK_ID)).thenReturn(paramExpression);
  //
  //    JpaPath<?> path = mock(JpaPath.class);
  //    doReturn(path).when(dataset).get(FieldNames.RUN_ID_DB);
  //
  //    JpaPredicate inPredicate = mock(JpaPredicate.class);
  //    when(path.in(paramExpression)).thenReturn(inPredicate);
  //
  //    JpaPredicate notPredicate = mock(JpaPredicate.class);
  //    when(criteriaBuilder.not(inPredicate)).thenReturn(notPredicate);
  //
  //    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary));
  //
  //    // When
  //    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);
  //
  //    // Then
  //    assertNotNull(result);
  //    assertEquals(excludedCheckIds, result.getExcludedCheckId());
  //  }

  @Test
  void findDatasetsSummaryFilterOptions_preservesDateFrom() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();

    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    LocalDate testDate = LocalDate.now();
    inputFilters.setDateFrom(testDate);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);

    DatasetSummary datasetSummary = new DatasetSummary("dataset", "Dataset", 100L, LocalDate.now(),
        "provider", "dataProvider", 75);

    JpaParameterExpression<Long> fromParamExpression = mock(JpaParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.STARTING_TIME_DB)).thenReturn(fromParamExpression);
    JpaPredicate fromPredicate = mock(JpaPredicate.class);
    JpaPath<Long> path = mock(JpaPath.class);
    doReturn(path).when(dataset).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.greaterThanOrEqualTo(path, fromParamExpression)).thenReturn(fromPredicate);
    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(testDate, result.getDateFrom());
  }

  @Test
  void findDatasetsSummaryFilterOptions_preservesDateTo() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    FieldFilters inputFilters = new FieldFilters();
    LocalDate testDate = LocalDate.now();
    inputFilters.setLimit(5);
    inputFilters.setOffset(0);
    inputFilters.setDateTo(testDate);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);

    DatasetSummary datasetSummary = new DatasetSummary("dataset", "Dataset", 100L, LocalDate.now(),
        "provider", "dataProvider", 75);

    JpaParameterExpression<Long> toParamExpression = mock(JpaParameterExpression.class);
    when(criteriaBuilder.parameter(Long.class, FieldNames.ENDING_TIME_DB)).thenReturn(toParamExpression);
    JpaPredicate toPredicate = mock(JpaPredicate.class);
    JpaPath<Long> path = mock(JpaPath.class);
    doReturn(path).when(dataset).get(FieldNames.STARTING_TIME_DB);
    when(criteriaBuilder.lessThan(path, toParamExpression)).thenReturn(toPredicate);
    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(testDate, result.getDateTo());
  }

  @Test
  void findDatasetsSummaryFilterOptions_preservesPercentLinksInOperationFrom() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    FieldFilters inputFilters = new FieldFilters();
    Integer percentFrom = 25;
    inputFilters.setPercentLinksInOperationFrom(percentFrom);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);

    DatasetSummary datasetSummary = new DatasetSummary("dataset", "Dataset", 100L, LocalDate.now(),
        "provider", "dataProvider", 75);

    JpaParameterExpression<Integer> fromParamExpression = mock(JpaParameterExpression.class);
    when(criteriaBuilder.parameter(Integer.class, FieldNames.PERCENT_LINKS_IN_OPERATION_FROM_DB)).thenReturn(fromParamExpression);

    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(percentFrom, result.getPercentLinksInOperationFrom());
  }

  @Test
  void findDatasetsSummaryFilterOptions_preservesPercentLinksInOperationTo() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    FieldFilters inputFilters = new FieldFilters();
    Integer percentTo = 75;
    inputFilters.setPercentLinksInOperationTo(percentTo);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);

    DatasetSummary datasetSummary = new DatasetSummary("dataset", "Dataset", 100L, LocalDate.now(),
        "provider", "dataProvider", 75);

    JpaParameterExpression<Integer> toParamExpression = mock(JpaParameterExpression.class);
    when(criteriaBuilder.parameter(Integer.class, FieldNames.PERCENT_LINKS_IN_OPERATION_TO)).thenReturn(toParamExpression);
    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(percentTo, result.getPercentLinksInOperationTo());
  }

  @Test
  void findDatasetsSummaryFilterOptions_preservesOffset() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    FieldFilters inputFilters = new FieldFilters();
    Integer offset = 10;
    inputFilters.setOffset(offset);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);

    DatasetSummary datasetSummary = new DatasetSummary("dataset", "Dataset", 100L, LocalDate.now(),
        "provider", "dataProvider", 75);

    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(offset, result.getOffset());
  }

  @Test
  void findDatasetsSummaryFilterOptions_preservesLimit() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    FieldFilters inputFilters = new FieldFilters();
    Integer limit = 5;
    inputFilters.setLimit(limit);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);

    DatasetSummary datasetSummary = new DatasetSummary("dataset", "Dataset", 100L, LocalDate.now(),
        "provider", "dataProvider", 75);

    when(query.getResultStream()).thenReturn(Stream.of(datasetSummary));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(limit, result.getLimit());
  }

  @Test
  void findDatasetsSummaryFilterOptions_createsNewFieldFiltersWithCollectedValues() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(2);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary summary1 = new DatasetSummary("dataset1", "Dataset1", 100L, LocalDate.now(),
        "provider1", "dataProvider1", 75);
    DatasetSummary summary2 = new DatasetSummary("dataset2", "Dataset2", 200L, LocalDate.now(),
        "provider2", "dataProvider2", 80);

    when(query.getResultStream()).thenReturn(Stream.of(summary1, summary2));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(2, result.getProvider().size());
    assertEquals(2, result.getDataProvider().size());
    assertEquals(2, result.getDatasetId().size());
    assertEquals(2, result.getDatasetName().size());
  }

  @Test
  void findDatasetsSummaryFilterOptions_handlesAllClioFilterFieldTypes() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(1);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary summary = new DatasetSummary("datasetId", "datasetName", 100L, LocalDate.now(),
        "provider", "dataProvider", 75);

    when(query.getResultStream()).thenReturn(Stream.of(summary));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertTrue(result.getProvider().contains("provider"));
    assertTrue(result.getDataProvider().contains("dataProvider"));
    assertTrue(result.getDatasetId().contains("datasetId"));
    assertTrue(result.getDatasetName().contains("datasetName"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_deduplicatesFilterValues() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters.setLimit(3);
    inputFilters.setOffset(0);
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);

    DatasetSummary summary1 = new DatasetSummary("ds1", "Name", 100L, LocalDate.now(),
        "provider", "dataProv", 75);
    DatasetSummary summary2 = new DatasetSummary("ds1", "Name", 200L, LocalDate.now(),
        "provider", "dataProv", 80);
    DatasetSummary summary3 = new DatasetSummary("ds1", "Name", 300L, LocalDate.now(),
        "provider", "dataProv", 85);

    when(query.getResultStream()).thenReturn(Stream.of(summary1, summary2, summary3));

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertEquals(1, result.getDatasetId().size());
    assertTrue(result.getDatasetId().contains("ds1"));
  }

  @Test
  void findDatasetsSummaryFilterOptions_returnsEmptySetsForNullOrEmptyResults() throws Exception {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    DatasetDao datasetDao = new DatasetDao(sessionFactory);
    FieldFilters inputFilters = new FieldFilters();
    inputFilters = FieldFilters.sanitizeFieldFilters(inputFilters);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class);
    JpaJoin<DatasetRow, RunRow> dataset = mock(JpaJoin.class);
    Query query = mockQuery(sessionFactory, session, criteriaBuilder, dataset);
    when(query.getResultStream()).thenReturn(Stream.of());

    // When
    FieldFilters result = datasetDao.findDatasetsSummaryFilterOptions(inputFilters);

    // Then
    assertNotNull(result);
    assertTrue(result.getProvider() == null || result.getProvider().isEmpty());
  }
}
