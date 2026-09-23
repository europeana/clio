package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.model.DatasetCheckSummary;
import eu.europeana.clio.common.model.DatasetSummary;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.Session;
import org.springframework.util.CollectionUtils;

/**
 * The type Dataset dao support.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DatasetDaoSupport {

  public static final double HUNDRED = 100.0D;
  public static final long NINETY_DAYS_PERIOD_WINDOW = 89L;

  /**
   * Add predicate and parameter date range.
   *
   * @param filters the filters
   * @param criteriaBuilder the criteria builder
   * @param predicates the wherePredicates
   * @param dataset the dataset
   * @param parametersMap the query parameters map
   */
  public static void addPredicateAndParameterDateRange(FieldFilters filters, CriteriaBuilder criteriaBuilder,
      List<Predicate> predicates, Join<DatasetRow, RunRow> dataset, Map<ParameterExpression<?>, Object> parametersMap) {
    if (filters.getDateFrom() != null) {
      ParameterExpression<Long> dateFromParameter = criteriaBuilder.parameter(Long.class, FieldNames.STARTING_TIME_DB);
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(dataset.get(FieldNames.DATASET_LAST_INDEX), dateFromParameter));
      parametersMap.put(dateFromParameter, filters.getDateFrom().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
    }

    if (filters.getDateTo() != null) {
      ParameterExpression<Long> dateToParameter = criteriaBuilder.parameter(Long.class, FieldNames.ENDING_TIME_DB);
      predicates.add(criteriaBuilder.lessThan(dataset.get(FieldNames.DATASET_LAST_INDEX), dateToParameter));
      parametersMap.put(dateToParameter,
          filters.getDateTo().plusDays(1L).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
    }
  }

  /**
   * Add predicate and parameter last three months.
   *
   * @param criteriaBuilder the criteria builder
   * @param predicates the predicates
   * @param link the link
   * @param parametersMap the parameters map
   */
  public static void addPredicateAndParameterLastThreeMonths(CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
      Root<LinkRow> link, Map<ParameterExpression<?>, Object> parametersMap) {
    LocalDate filterPeriodWindow = LocalDate.now(ZoneOffset.UTC);
    long startingWindowTime = filterPeriodWindow.minusDays(NINETY_DAYS_PERIOD_WINDOW).atStartOfDay(ZoneOffset.UTC).toInstant()
                                                .toEpochMilli();
    long endingWindowTime = filterPeriodWindow.plusDays(1L).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    ParameterExpression<Long> dateFromParameter = criteriaBuilder.parameter(Long.class, FieldNames.STARTING_WINDOW_TIME_DB);
    predicates.add(criteriaBuilder.greaterThanOrEqualTo(link.get(FieldNames.LINK_CHECKING_TIME), dateFromParameter));
    parametersMap.put(dateFromParameter, startingWindowTime);

    ParameterExpression<Long> dateToParameter = criteriaBuilder.parameter(Long.class, FieldNames.ENDING_WINDOW_TIME_DB);
    predicates.add(criteriaBuilder.lessThan(link.get(FieldNames.LINK_CHECKING_TIME), dateToParameter));
    parametersMap.put(dateToParameter, endingWindowTime);
  }

  /**
   * Add predicate and parameter excluded ids.
   *
   * @param fieldValue the field value
   * @param criteriaBuilder the criteria builder
   * @param predicates the wherePredicates
   * @param dataset the dataset
   * @param parametersMap the query parameters map
   */
  public static void addPredicateAndParameterExcludedIds(Set<String> fieldValue, CriteriaBuilder criteriaBuilder,
      List<Predicate> predicates, Join<DatasetRow, RunRow> dataset, Map<ParameterExpression<?>, Object> parametersMap) {

    if (!CollectionUtils.isEmpty(fieldValue)) {
      ParameterExpression<Set> excludeCheckIdsParameter = criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_ID);
      predicates.add(criteriaBuilder.not(dataset.get(FieldNames.DATASET_ID_DB).in(excludeCheckIdsParameter)));
      parametersMap.put(excludeCheckIdsParameter, fieldValue);
    }
  }

  /**
   * Add predicate percent links in operation.
   *
   * @param filters the filters
   * @param criteriaBuilder the criteria builder
   * @param predicates the wherePredicates
   * @param expressionPercentage the expression percentage
   * @param parametersMap the query parameters map
   */
  public static void addPredicatePercentLinksInOperation(FieldFilters filters, CriteriaBuilder criteriaBuilder,
      List<Predicate> predicates, Expression<Integer> expressionPercentage, Map<ParameterExpression<?>, Object> parametersMap) {
    if (filters.getPercentLinksInOperationFrom() != null) {
      ParameterExpression<Integer> percentLinksInOperationParameter = criteriaBuilder.parameter(Integer.class,
          FieldNames.PERCENT_LINKS_IN_OPERATION_FROM_DB);
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(expressionPercentage, percentLinksInOperationParameter));
      parametersMap.put(percentLinksInOperationParameter, filters.getPercentLinksInOperationFrom());
    }
    if (filters.getPercentLinksInOperationTo() != null) {
      ParameterExpression<Integer> percentLinksInOperationParameter = criteriaBuilder.parameter(Integer.class,
          FieldNames.PERCENT_LINKS_IN_OPERATION_TO_DB);
      predicates.add(criteriaBuilder.lessThanOrEqualTo(expressionPercentage, percentLinksInOperationParameter));
      parametersMap.put(percentLinksInOperationParameter, filters.getPercentLinksInOperationTo());
    }
  }

  /**
   * Add predicate and parameter.
   *
   * @param fieldValue the field value
   * @param criteriaBuilder the criteria builder
   * @param predicates the wherePredicates
   * @param dataset the dataset
   * @param parametersMap the parameter's map
   * @param fieldName the field name
   */
  public static void addPredicateAndParameter(Set<String> fieldValue, CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
      Join<DatasetRow, RunRow> dataset, Map<ParameterExpression<?>, Object> parametersMap, String fieldName) {
    if (!(fieldValue == null || fieldValue.isEmpty())) {
      ParameterExpression<Set> parameter = criteriaBuilder.parameter(Set.class, fieldName + "Parameter");
      predicates.add(dataset.get(fieldName).in(parameter));
      parametersMap.put(parameter, fieldValue);
    }
  }

  /**
   * Builds common query parts for check runs queries with all predicates and aggregations. This method handles the construction
   * of a criteria query with all standard filters.
   *
   * @param <T> the type parameter
   * @param criteriaBuilder the criteria builder
   * @param clazz the result class
   * @param filters the field filters to apply
   * @return common query parts with predicates already applied
   */
  public static <T> CommonDatasetQueryParts<T> buildCommonDatasetQueryWithPredicates(CriteriaBuilder criteriaBuilder,
      Class<T> clazz, FieldFilters filters) {
    // Build base query parts
    CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
    Root<LinkRow> link = criteriaQuery.from(LinkRow.class);
    Join<RunRow, DatasetRow> run = link.join("run", JoinType.INNER);
    Join<DatasetRow, RunRow> dataset = run.join("dataset", JoinType.INNER);

    List<Predicate> wherePredicates = new ArrayList<>();
    List<Predicate> havingPredicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();

    // Compute aggregations
    Expression<Long> errorsLinks = criteriaBuilder.coalesce(criteriaBuilder.count(link.get(FieldNames.ERROR_MESSAGE_DB)), 0)
                                                  .as(Long.class);
    Expression<Long> totalLinks = criteriaBuilder.coalesce(criteriaBuilder.count(link), 0).as(Long.class);

    Expression<Integer> percentLinksInOperation = criteriaBuilder.diff(HUNDRED, criteriaBuilder.prod(
        criteriaBuilder.<Double>selectCase().when(criteriaBuilder.equal(totalLinks, 0D), 0D).otherwise(
            criteriaBuilder.quot(criteriaBuilder.toDouble(errorsLinks), criteriaBuilder.toDouble(totalLinks)).as(Double.class)),
        HUNDRED)).cast(Integer.class);

    // Apply filters
    addPredicateAndParameter(filters.getProvider(), criteriaBuilder, wherePredicates, dataset, parametersMap,
        FieldNames.PROVIDER);
    addPredicateAndParameter(filters.getDataProvider(), criteriaBuilder, wherePredicates, dataset, parametersMap,
        FieldNames.DATA_PROVIDER);
    addPredicateAndParameter(filters.getDatasetId(), criteriaBuilder, wherePredicates, dataset, parametersMap,
        FieldNames.DATASET_ID);
    addPredicateAndParameter(filters.getDatasetName(), criteriaBuilder, wherePredicates, dataset, parametersMap,
        FieldNames.DATASET_NAME_DB);
    addPredicateAndParameterExcludedIds(filters.getExcludedId(), criteriaBuilder, wherePredicates, dataset, parametersMap);
    addPredicateAndParameterLastThreeMonths(criteriaBuilder, wherePredicates, link, parametersMap);
    addPredicateAndParameterDateRange(filters, criteriaBuilder, wherePredicates, dataset, parametersMap);
    addPredicatePercentLinksInOperation(filters, criteriaBuilder, havingPredicates, percentLinksInOperation, parametersMap);

    return new CommonDatasetQueryParts<>(criteriaQuery, link, run, dataset, errorsLinks, totalLinks, percentLinksInOperation,
        wherePredicates, havingPredicates, parametersMap);
  }

  /**
   * Gets dataset summary typed query.
   *
   * @param filters the filters
   * @param session the session
   * @return the dataset summary typed query
   */
  public static TypedQuery<DatasetSummary> getDatasetSummaryTypedQuery(FieldFilters filters, Session session) {
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CommonDatasetQueryParts<DatasetSummary> queryParts = buildCommonDatasetQueryWithPredicates(criteriaBuilder,
        DatasetSummary.class, filters);

    CriteriaQuery<DatasetSummary> criteriaQuery = queryParts.criteriaQuery();

    // select
    criteriaQuery.select(criteriaBuilder.construct(DatasetSummary.class, queryParts.dataset().get(FieldNames.DATASET_ID_DB),
        queryParts.dataset().get(FieldNames.DATASET_NAME_DB), queryParts.dataset().get(FieldNames.DATASET_SIZE),
        queryParts.dataset().get(FieldNames.DATASET_LAST_INDEX), queryParts.dataset().get(FieldNames.PROVIDER),
        queryParts.dataset().get(FieldNames.DATA_PROVIDER),
        queryParts.percentLinksInOperation().alias(FieldNames.PERCENT_LINKS_IN_OPERATION_DB)));

    // where & having
    criteriaQuery.where(criteriaBuilder.and(queryParts.wherePredicates()));
    criteriaQuery.having(queryParts.havingPredicates());

    // order by (specific to LinkDao)
    criteriaQuery.orderBy(criteriaBuilder.asc(queryParts.dataset().get(FieldNames.DATASET_ID_DB)));

    // group by
    criteriaQuery.groupBy(queryParts.dataset().get(FieldNames.DATASET_ID_DB),
        queryParts.dataset().get(FieldNames.DATASET_NAME_DB), queryParts.dataset().get(FieldNames.DATASET_SIZE),
        queryParts.dataset().get(FieldNames.DATASET_LAST_INDEX), queryParts.dataset().get(FieldNames.PROVIDER),
        queryParts.dataset().get(FieldNames.DATA_PROVIDER));

    // execute query
    TypedQuery<DatasetSummary> query = session.createQuery(criteriaQuery);
    queryParts.parametersMap().forEach((key, value) -> query.setParameter(key.getName(), value));
    return query;
  }

  /**
   * Represents the common components of a run summary criteria query.
   *
   * @param <T> the type parameter e.g., a RunSummary
   */
  public record CommonDatasetQueryParts<T>(
      CriteriaQuery<T> criteriaQuery,
      Root<LinkRow> link,
      Join<RunRow, DatasetRow> run,
      Join<DatasetRow, RunRow> dataset,
      Expression<Long> errorsLinks,
      Expression<Long> totalLinks,
      Expression<Integer> percentLinksInOperation,
      List<Predicate> wherePredicates,
      List<Predicate> havingPredicates,
      Map<ParameterExpression<?>, Object> parametersMap) {

  }

  /**
   * Build common dataset checks query with predicates common dataset query parts.
   *
   * @param <T> the type parameter
   * @param criteriaBuilder the criteria builder
   * @param clazz the clazz
   * @param filters the filters
   * @return the common dataset query parts
   */
  public static <T> CommonDatasetQueryParts<T> buildCommonDatasetChecksQueryWithPredicates(CriteriaBuilder criteriaBuilder,
      Class<T> clazz, FieldFilters filters) {
    // Build base query parts
    CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
    Root<LinkRow> link = criteriaQuery.from(LinkRow.class);
    Join<RunRow, DatasetRow> run = link.join("run", JoinType.INNER);

    List<Predicate> wherePredicates = new ArrayList<>();
    List<Predicate> havingPredicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();

    // Compute aggregations
    Expression<Long> errorsLinks = criteriaBuilder.coalesce(criteriaBuilder.count(link.get(FieldNames.ERROR_MESSAGE_DB)), 0)
                                                  .as(Long.class);
    Expression<Long> totalLinks = criteriaBuilder.coalesce(criteriaBuilder.count(link), 0).as(Long.class);

    Expression<Integer> percentLinksInOperation = criteriaBuilder.diff(HUNDRED, criteriaBuilder.prod(
        criteriaBuilder.<Double>selectCase().when(criteriaBuilder.equal(totalLinks, 0D), 0D).otherwise(
            criteriaBuilder.quot(criteriaBuilder.toDouble(errorsLinks), criteriaBuilder.toDouble(totalLinks)).as(Double.class)),
        HUNDRED)).cast(Integer.class);

    // Apply filters
    String datasetId = filters.getDatasetId().first();
    ParameterExpression<String> parameter = criteriaBuilder.parameter(String.class, FieldNames.DATASET_ID_DB + "Parameter");
    wherePredicates.add(run.get("dataset").get(FieldNames.DATASET_ID_DB).equalTo(parameter));
    parametersMap.put(parameter, datasetId);

    addPredicateAndParameterLastThreeMonths(criteriaBuilder, wherePredicates, link, parametersMap);

    addPredicatePercentLinksInOperation(filters, criteriaBuilder, havingPredicates, percentLinksInOperation, parametersMap);

    return new CommonDatasetQueryParts<>(criteriaQuery, link, run, null, errorsLinks, totalLinks, percentLinksInOperation,
        wherePredicates, havingPredicates, parametersMap);
  }

  /**
   * Gets dataset check summary typed query.
   *
   * @param filters the filters
   * @param session the session
   * @return the dataset check summary typed query
   */
  public static TypedQuery<DatasetCheckSummary> getDatasetCheckSummaryTypedQuery(FieldFilters filters, Session session) {
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CommonDatasetQueryParts<DatasetCheckSummary> queryParts = buildCommonDatasetChecksQueryWithPredicates(criteriaBuilder,
        DatasetCheckSummary.class, filters);

    CriteriaQuery<DatasetCheckSummary> criteriaQuery = queryParts.criteriaQuery();

    // select
    criteriaQuery.select(criteriaBuilder.construct(DatasetCheckSummary.class, queryParts.run().get(FieldNames.RUN_ID_DB),
        queryParts.run().get(FieldNames.STARTING_TIME_DB),
        queryParts.percentLinksInOperation().alias(FieldNames.PERCENT_LINKS_IN_OPERATION_DB)));

    // where & having
    criteriaQuery.where(criteriaBuilder.and(queryParts.wherePredicates()));
    criteriaQuery.having(queryParts.havingPredicates());

    // order by (specific to LinkDao)
    criteriaQuery.orderBy(criteriaBuilder.desc(queryParts.run().get(FieldNames.STARTING_TIME_DB)));

    // group by
    criteriaQuery.groupBy(queryParts.run().get(FieldNames.STARTING_TIME_DB), queryParts.run().get(FieldNames.RUN_ID_DB));

    // execute query
    TypedQuery<DatasetCheckSummary> query = session.createQuery(criteriaQuery);
    queryParts.parametersMap().forEach((key, value) -> query.setParameter(key.getName(), value));
    return query;
  }
}
