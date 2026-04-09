package eu.europeana.clio.common.persistence.dao;

import static java.lang.String.format;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.RunSummary;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.model.BatchRow;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.springframework.util.CollectionUtils;

/**
 * Data access object for runs (a checking iteration for a given dataset).
 */
public class RunDao {

  public static final double HUNDRED = 100.0D;
  private final HibernateSessionUtils hibernateSessionUtils;

  /**
   * Constructor.
   *
   * @param sessionFactory The connection to the Clio persistence. Should be connected. This object does not close the
   * connection.
   */
  public RunDao(SessionFactory sessionFactory) {
    this.hibernateSessionUtils = new HibernateSessionUtils(sessionFactory);
  }

  /**
   * Add predicate and parameter date range.
   *
   * @param filters the filters
   * @param criteriaBuilder the criteria builder
   * @param predicates the wherePredicates
   * @param run the run
   * @param parametersMap the query parameters map
   */
  public static void addPredicateAndParameterDateRange(FieldFilters filters, CriteriaBuilder criteriaBuilder,
      List<Predicate> predicates,
      Join<LinkRow, RunRow> run, Map<ParameterExpression<?>, Object> parametersMap) {
    if (filters.getDateFrom() != null) {
      ParameterExpression<Long> dateFromParameter = criteriaBuilder.parameter(Long.class, FieldNames.STARTING_TIME_DB);
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(run.get(FieldNames.STARTING_TIME_DB), dateFromParameter));
      parametersMap.put(dateFromParameter, filters.getDateFrom().toInstant().toEpochMilli());
    }

    if (filters.getDateTo() != null) {
      ParameterExpression<Long> dateToParameter = criteriaBuilder.parameter(Long.class, FieldNames.ENDING_TIME_DB);
      predicates.add(criteriaBuilder.lessThan(run.get(FieldNames.STARTING_TIME_DB), dateToParameter));
      parametersMap.put(dateToParameter, filters.getDateTo().toInstant().plus(Duration.ofDays(1)).toEpochMilli());
    }
  }

  /**
   * Add predicate and parameter excluded ids.
   *
   * @param fieldValue the field value
   * @param criteriaBuilder the criteria builder
   * @param predicates the wherePredicates
   * @param run the run
   * @param parametersMap the query parameters map
   */
  public static void addPredicateAndParameterExcludedIds(Set<Long> fieldValue, CriteriaBuilder criteriaBuilder,
      List<Predicate> predicates,
      Join<LinkRow, RunRow> run, Map<ParameterExpression<?>, Object> parametersMap) {

    if (!CollectionUtils.isEmpty(fieldValue)) {
      ParameterExpression<Set> excludeCheckIdsParameter = criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_CHECK_ID);
      predicates.add(criteriaBuilder.not(run.get(FieldNames.RUN_ID_DB).in(excludeCheckIdsParameter)));
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
      List<Predicate> predicates,
      Expression<Integer> expressionPercentage, Map<ParameterExpression<?>, Object> parametersMap) {
    if (filters.getPercentLinksInOperationFrom() != null) {
      ParameterExpression<Integer> percentLinksInOperationParameter = criteriaBuilder.parameter(Integer.class,
          FieldNames.PERCENT_LINKS_IN_OPERATION_FROM_DB);
      predicates.add(criteriaBuilder.ge(expressionPercentage, percentLinksInOperationParameter));
      parametersMap.put(percentLinksInOperationParameter, filters.getPercentLinksInOperationFrom());
    }
    if (filters.getPercentLinksInOperationTo() != null) {
      ParameterExpression<Integer> percentLinksInOperationParameter = criteriaBuilder.parameter(Integer.class,
          FieldNames.PERCENT_LINKS_IN_OPERATION_TO_DB);
      predicates.add(criteriaBuilder.lt(expressionPercentage, percentLinksInOperationParameter));
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
      Join<RunRow, DatasetRow> dataset, Map<ParameterExpression<?>, Object> parametersMap, String fieldName) {
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
   * @param criteriaBuilder the criteria builder
   * @param clazz the result class
   * @param filters the field filters to apply
   * @return common query parts with predicates already applied
   */
  public static <T> CommonRunSummaryQueryParts<T> buildCommonRunSummaryQueryWithPredicates(
      CriteriaBuilder criteriaBuilder, Class<T> clazz, FieldFilters filters) {
    // Build base query parts
    CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
    Root<LinkRow> link = criteriaQuery.from(LinkRow.class);
    Join<LinkRow, RunRow> run = link.join("run", JoinType.INNER);
    Join<RunRow, DatasetRow> dataset = run.join("dataset", JoinType.INNER);
    Join<RunRow, BatchRow> batch = run.join("batch", JoinType.INNER);
    List<Predicate> wherePredicates = new ArrayList<>();
    List<Predicate> havingPredicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();

    // Compute aggregations
    Expression<Long> errorsLinks = criteriaBuilder.coalesce(criteriaBuilder.count(link.get(FieldNames.ERROR_MESSAGE_DB)),0).as(Long.class);
    Expression<Long> totalLinks = criteriaBuilder.coalesce(criteriaBuilder.count(link),0).as(Long.class);

    Expression<Integer> percentLinksInOperation = criteriaBuilder.diff(HUNDRED,
        criteriaBuilder.prod(
            criteriaBuilder.<Double>selectCase()
                               .when(criteriaBuilder.equal(totalLinks,0D), 0D)
                               .otherwise(criteriaBuilder.quot(
                                   criteriaBuilder.toDouble(errorsLinks),
                                   criteriaBuilder.toDouble(totalLinks)
                               ).as(Double.class)),
            HUNDRED
        )).cast(Integer.class);

    // Apply filters
    addPredicateAndParameter(filters.getProvider(), criteriaBuilder, wherePredicates,
        dataset, parametersMap, FieldNames.PROVIDER);
    addPredicateAndParameter(filters.getDataProvider(), criteriaBuilder, wherePredicates,
        dataset, parametersMap, FieldNames.DATA_PROVIDER);
    addPredicateAndParameter(filters.getDatasetId(), criteriaBuilder, wherePredicates,
        dataset, parametersMap, FieldNames.DATASET_ID);
    addPredicateAndParameter(filters.getDatasetName(), criteriaBuilder, wherePredicates,
        dataset, parametersMap, FieldNames.DATASET_NAME_DB);
    addPredicateAndParameterExcludedIds(filters.getExcludedCheckId(), criteriaBuilder,
        wherePredicates, run, parametersMap);
    addPredicateAndParameterDateRange(filters, criteriaBuilder, wherePredicates,
        run, parametersMap);
    addPredicatePercentLinksInOperation(filters, criteriaBuilder, havingPredicates,
        percentLinksInOperation, parametersMap);

    return new CommonRunSummaryQueryParts<>(
        criteriaQuery,
        link,
        run,
        dataset,
        batch,
        errorsLinks,
        totalLinks,
        percentLinksInOperation,
        wherePredicates,
        havingPredicates,
        parametersMap
    );
  }

  /**
   * Create a run with a starting time equal to the current time.
   *
   * @param datasetId The (Metis) dataset ID of the dataset to which this run belongs.
   * @param batchId The ID of the batch to which this run belongs.
   * @return The ID of the run.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public long createRunStartingNow(String datasetId, long batchId) throws PersistenceException {
    return hibernateSessionUtils.performInTransaction(session -> {
      final DatasetRow datasetRow = session.find(DatasetRow.class, datasetId);
      if (datasetRow == null) {
        throw new PersistenceException(format("Cannot create run: dataset with ID %s does not exist.", datasetId));
      }
      final BatchRow batchRow = session.find(BatchRow.class, batchId);
      if (batchRow == null) {
        throw new PersistenceException(format("Cannot create run: batch with ID %s does not exist.", batchId));
      }
      final RunRow newRun = new RunRow(Instant.now(), datasetRow, batchRow);
      session.persist(newRun);
      session.flush();
      return newRun.getRunId();
    });
  }

  /**
   * Determines whether the dataset in question currently has an active run (i.e. a run for which at least one link has not been
   * checked yet).
   *
   * @param datasetId The dataset ID for which to check.
   * @return Whether there is an active run.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public boolean datasetHasActiveRun(String datasetId) throws PersistenceException {
    return hibernateSessionUtils.performInSession(
        session -> !session.createNamedQuery(RunRow.GET_ACTIVE_RUN_FOR_DATASET, RunRow.class)
                           .setParameter(RunRow.DATASET_ID_PARAMETER, datasetId).getResultList()
                           .isEmpty());
  }

  /**
   * Finds runs summary.
   *
   * @param filters the filters
   * @return the check runs
   * @throws PersistenceException the persistence exception
   */
  public List<RunSummary> findRunsSummary(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performInSession(session -> {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CommonRunSummaryQueryParts<RunSummary> queryParts = buildCommonRunSummaryQueryWithPredicates(
          criteriaBuilder, RunSummary.class, filters);

      CriteriaQuery<RunSummary> criteriaQuery = queryParts.criteriaQuery();
      Expression<Long> startingTime = criteriaBuilder.min(queryParts.run().get(FieldNames.STARTING_TIME_DB));

      // select
      criteriaQuery.select(criteriaBuilder.construct(
          RunSummary.class,
          queryParts.run().get(FieldNames.RUN_ID_DB),
          startingTime.alias(FieldNames.STARTING_TIME_DB),
          queryParts.dataset().get(FieldNames.DATASET_ID_DB),
          queryParts.dataset().get(FieldNames.DATASET_NAME_DB),
          queryParts.dataset().get(FieldNames.DATASET_SIZE),
          queryParts.dataset().get(FieldNames.DATASET_LAST_INDEX),
          queryParts.dataset().get(FieldNames.PROVIDER),
          queryParts.dataset().get(FieldNames.DATA_PROVIDER),
          queryParts.percentLinksInOperation().alias(FieldNames.PERCENT_LINKS_IN_OPERATION_DB)
      ));

      // where & having
      criteriaQuery.where(criteriaBuilder.and(queryParts.wherePredicates()));
      criteriaQuery.having(queryParts.havingPredicates());

      // order by (specific to LinkDao)
      criteriaQuery.orderBy(criteriaBuilder.asc(queryParts.run().get(FieldNames.RUN_ID_DB)));

      // group by
      criteriaQuery.groupBy(
          queryParts.batch().get(FieldNames.BATCH_ID_DB),
          queryParts.run().get(FieldNames.RUN_ID_DB),
          queryParts.dataset().get(FieldNames.DATASET_ID_DB)
      );

      // execute query
      TypedQuery<RunSummary> query = session.createQuery(criteriaQuery);
      queryParts.parametersMap().forEach((key, value) -> query.setParameter(key.getName(), value));

      return query.setFirstResult(filters.getOffset())
                  .setMaxResults(filters.getLimit())
                  .getResultList();
    });
  }

  /**
   * Represents the common components of a run summary criteria query.
   *
   * @param <T> the type parameter e.g., a RunSummary
   * @param criteriaQuery the criteria query
   * @param link the link
   * @param run the run
   * @param dataset the dataset
   * @param batch the batch
   * @param errorsLinks the error's link
   * @param totalLinks the total links
   * @param percentLinksInOperation the percent links in operation
   * @param wherePredicates the where predicates
   * @param havingPredicates the having predicates
   * @param parametersMap the parameter map
   */
  public record CommonRunSummaryQueryParts<T>(
      CriteriaQuery<T> criteriaQuery,
      Root<LinkRow> link,
      Join<LinkRow, RunRow> run,
      Join<RunRow, DatasetRow> dataset,
      Join<RunRow, BatchRow> batch,
      Expression<Long> errorsLinks,
      Expression<Long> totalLinks,
      Expression<Integer> percentLinksInOperation,
      List<Predicate> wherePredicates,
      List<Predicate> havingPredicates,
      Map<ParameterExpression<?>, Object> parametersMap) {
  }

  /**
   * Convert run.
   *
   * @param row the row
   * @return the run
   */
  static Run convert(RunRow row) {
    return new Run(row.getRunId(), row.getStartingTime(), DatasetDao.convert(row.getDataset()));
  }
}
