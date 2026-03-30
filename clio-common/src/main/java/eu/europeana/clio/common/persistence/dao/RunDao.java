package eu.europeana.clio.common.persistence.dao;

import static java.lang.String.format;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.CheckRunRecord;
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
          FieldNames.PERCENT_LINKS_IN_OPERATION_DB+"Min");
      predicates.add(criteriaBuilder.ge(expressionPercentage, percentLinksInOperationParameter));
      parametersMap.put(percentLinksInOperationParameter, filters.getPercentLinksInOperationFrom());
    }
    if (filters.getPercentLinksInOperationTo() != null) {
      ParameterExpression<Integer> percentLinksInOperationParameter = criteriaBuilder.parameter(Integer.class,
          FieldNames.PERCENT_LINKS_IN_OPERATION_DB+"Max");
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
   * @param parametersMap the parameters map
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
   * Build check runs query parts This method builds the common parts of the criteria query for fetching check runs, including the
   * root, joins, and initial structures for wherePredicates and parameters.
   *
   * @param criteriaBuilder the criteria builder
   * @param clazz the clazz
   * @return the query parts
   */
  public static<T> QueryParts<T> buildCheckRunsQueryParts(CriteriaBuilder criteriaBuilder, Class<T> clazz) {
    CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(clazz);
    Root<LinkRow> link = criteriaQuery.from(LinkRow.class);
    Join<LinkRow, RunRow> run = link.join("run", JoinType.INNER);
    Join<RunRow, DatasetRow> dataset = run.join("dataset", JoinType.INNER);
    Join<RunRow, BatchRow> batch = run.join("batch", JoinType.INNER);
    List<Predicate> wherePredicates = new ArrayList<>();
    List<Predicate> havingPredicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    return new QueryParts<>(criteriaQuery, link, run, dataset, batch, wherePredicates, havingPredicates, parametersMap);
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
   * Finds check runs.
   *
   * @param filters the filters
   * @return the check runs
   * @throws PersistenceException the persistence exception
   */
  public List<CheckRunRecord> findCheckRuns(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performInSession(session -> {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      QueryParts<CheckRunRecord> parts = buildCheckRunsQueryParts(criteriaBuilder, CheckRunRecord.class);
      CriteriaQuery<CheckRunRecord> criteriaQuery = parts.criteriaQuery();
      Root<LinkRow> link = parts.link();
      Join<LinkRow, RunRow> run = parts.run();
      Join<RunRow, DatasetRow> dataset = parts.dataset();
      Join<RunRow, BatchRow> batch = parts.batch();
      List<Predicate> wherePredicates = parts.wherePredicates();
      List<Predicate> havingPredicates = parts.havingPredicates();
      Map<ParameterExpression<?>, Object> parametersMap = parts.parametersMap();

      // aggregations
      Expression<Long> errorsLinks = criteriaBuilder.count(link.get(FieldNames.ERROR_MESSAGE_DB));
      Expression<Long> totalLinks = criteriaBuilder.count(link.get("run").get(FieldNames.RUN_ID_DB));
      Expression<Long> startingTime = criteriaBuilder.min(run.get(FieldNames.STARTING_TIME_DB));
      Expression<Integer> percentLinksInOperation = criteriaBuilder.diff(HUNDRED,
          criteriaBuilder.prod(
              criteriaBuilder.quot(
                  criteriaBuilder.toDouble(errorsLinks),
                  criteriaBuilder.toDouble(criteriaBuilder.coalesce(totalLinks, 1))
              ),
              HUNDRED
          )).cast(Integer.class);

      // wherePredicates
      addPredicateAndParameter(filters.getProvider(), criteriaBuilder, wherePredicates, dataset, parametersMap,
          FieldNames.PROVIDER);
      addPredicateAndParameter(filters.getDataProvider(), criteriaBuilder, wherePredicates, dataset, parametersMap,
          FieldNames.DATA_PROVIDER);
      addPredicateAndParameter(filters.getDatasetId(), criteriaBuilder, wherePredicates, dataset, parametersMap,
          FieldNames.DATASET_ID);
      addPredicateAndParameter(filters.getDatasetName(), criteriaBuilder, wherePredicates, dataset, parametersMap,
          FieldNames.DATASET_NAME_DB);
      addPredicateAndParameterExcludedIds(filters.getExcludedCheckId(), criteriaBuilder, wherePredicates, run, parametersMap);
      addPredicateAndParameterDateRange(filters, criteriaBuilder, wherePredicates, run, parametersMap);
      addPredicatePercentLinksInOperation(filters, criteriaBuilder, havingPredicates, percentLinksInOperation, parametersMap);

      // AND combination
      Predicate whereClause = criteriaBuilder.and(wherePredicates);

      // select
      criteriaQuery.select(criteriaBuilder.construct(
          CheckRunRecord.class,
          run.get(FieldNames.RUN_ID_DB),
          startingTime.alias(FieldNames.STARTING_TIME_DB),
          dataset.get(FieldNames.DATASET_ID_DB),
          dataset.get(FieldNames.DATASET_NAME_DB),
          dataset.get(FieldNames.DATASET_SIZE),
          dataset.get(FieldNames.DATASET_LAST_INDEX),
          dataset.get(FieldNames.PROVIDER),
          dataset.get(FieldNames.DATA_PROVIDER),
          percentLinksInOperation.alias(FieldNames.PERCENT_LINKS_IN_OPERATION_DB)
      ));

      // where
      criteriaQuery.where(whereClause);
      criteriaQuery.having(havingPredicates);

      // group by
      criteriaQuery.groupBy(
          batch.get(FieldNames.BATCH_ID_DB),
          run.get(FieldNames.RUN_ID_DB),
          dataset.get(FieldNames.DATASET_ID_DB)
      );

      // create query

      TypedQuery<CheckRunRecord> query = session.createQuery(criteriaQuery);

      // set value to parameters
      parametersMap.forEach((key, value) -> query.setParameter(key.getName(), value));

      return query.setFirstResult(filters.getOffset())
                  .setMaxResults(filters.getLimit())
                  .getResultList();
    });
  }

  /**
   * Helper holder for parts used in criteria building.
   */
  public record QueryParts<T>(CriteriaQuery<T> criteriaQuery,
                           Root<LinkRow> link,
                           Join<LinkRow, RunRow> run,
                           Join<RunRow, DatasetRow> dataset,
                           Join<RunRow, BatchRow> batch,
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
