package eu.europeana.clio.common.persistence.dao;

import static java.lang.String.format;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.CheckRecord;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import jakarta.persistence.Tuple;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.SessionFactory;

/**
 * Data access object for runs (a checking iteration for a given dataset).
 */
public class RunDao {

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
        session -> !session.createNamedQuery(RunRow.GET_ACTIVE_RUN_FOR_DATASET)
                           .setParameter(RunRow.DATASET_ID_PARAMETER, datasetId).getResultList()
                           .isEmpty());
  }

  /**
   * Gets check runs.
   *
   * @param filters the filters
   * @return the check runs
   * @throws PersistenceException the persistence exception
   */
  public List<CheckRecord> getCheckRuns(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performInSession(session -> {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      QueryParts parts = buildCheckRunsQueryParts(criteriaBuilder);
      CriteriaQuery<Tuple> criteriaQuery = parts.criteriaQuery();
      Root<LinkRow> link = parts.link();
      Join<LinkRow, RunRow> run = parts.run();
      Join<RunRow, DatasetRow> dataset = parts.dataset();
      Join<RunRow, BatchRow> batch = parts.batch();
      List<Predicate> predicates = parts.predicates();
      Map<ParameterExpression<?>, Object> parametersMap = parts.parametersMap();

      // predicates
      addPredicateAndParameter(filters.getProvider(), criteriaBuilder, predicates, dataset, parametersMap, FieldNames.PROVIDER);
      addPredicateAndParameter(filters.getDataProvider(), criteriaBuilder, predicates, dataset, parametersMap, FieldNames.DATA_PROVIDER);
      addPredicateAndParameter(filters.getDatasetId(), criteriaBuilder, predicates, dataset, parametersMap, FieldNames.DATASET_ID);
      addPredicateAndParameter(filters.getDatasetName(), criteriaBuilder, predicates, dataset, parametersMap, FieldNames.DATASET_NAME_DB);
      addPredicateAndParameterExcludedIds(filters.getExcludedCheckIds(), criteriaBuilder, predicates, run, parametersMap);
      addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, run, parametersMap);

      // AND combination
      Predicate whereClause = criteriaBuilder.and(predicates);

      // aggregations
      Expression<Long> errorsLinks = criteriaBuilder.count(link.get(FieldNames.ERROR_MESSAGE_DB));
      Expression<Long> totalLinks = criteriaBuilder.count(run.get(FieldNames.RUN_ID_DB));
      Expression<Long> startingTime = criteriaBuilder.min(run.get(FieldNames.STARTING_TIME_DB));

      // select
      criteriaQuery.select(criteriaBuilder.tuple(
          run.get(FieldNames.RUN_ID_DB),
          dataset,
          startingTime.alias(FieldNames.STARTING_TIME_DB),
          errorsLinks.alias(FieldNames.ERROR_LINKS_DB),
          totalLinks.alias(FieldNames.TOTAL_LINKS_DB)
      ));

      // where
      criteriaQuery.where(whereClause);

      // group by
      criteriaQuery.groupBy(
          batch.get(FieldNames.BATCH_ID_DB),
          dataset.get(FieldNames.DATASET_ID_DB),
          run.get(FieldNames.RUN_ID_DB)
      );

      // create query
      TypedQuery<Tuple> query = session.createQuery(criteriaQuery);

      // set value to parameters
      parametersMap.forEach((key, value) -> query.setParameter(key.getName(), value));

      return query
          .getResultStream()
          .filter(tuple -> percentLinksInOperation(filters, tuple))
          .map(tuple -> {
            DatasetRow datasetRow = tuple.get(1, DatasetRow.class);
            var percentResult = (int) ((long) tuple.get(FieldNames.ERROR_LINKS_DB) /  ((long) tuple.get(FieldNames.TOTAL_LINKS_DB)) * 100);
            return new CheckRecord((long) tuple.get(0),
                new Date((long) tuple.get(FieldNames.STARTING_TIME_DB)),
                datasetRow.getDatasetId(),
                datasetRow.getName(),
                datasetRow.getSize(),
                new Date(datasetRow.getLastIndexTime().toEpochMilli()),
                datasetRow.getProvider(),
                datasetRow.getDataProvider(),
                percentResult);
          }).toList();
    });
  }

  /**
   * Add predicate and parameter date range.
   *
   * @param filters the filters
   * @param criteriaBuilder the criteria builder
   * @param predicates the predicates
   * @param run the run
   * @param parametersMap the parameters map
   */
  public static void addPredicateAndParameterDateRange(FieldFilters filters, CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
      Join<LinkRow, RunRow> run, Map<ParameterExpression<?>, Object> parametersMap) {
    if (filters.getDateFrom() != null) {
      ParameterExpression<Long> dateFromParameter = criteriaBuilder.parameter(Long.class, FieldNames.STARTING_TIME_DB);
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(run.get(FieldNames.STARTING_TIME_DB), dateFromParameter));
      parametersMap.put(dateFromParameter, filters.getDateFrom().toInstant().toEpochMilli());
    }

    if (filters.getDateTo() != null) {
      ParameterExpression<Long> dateToParameter = criteriaBuilder.parameter(Long.class, FieldNames.ENDING_TIME_DB);
      predicates.add(criteriaBuilder.lessThanOrEqualTo(run.get(FieldNames.STARTING_TIME_DB), dateToParameter));
      Duration addEndOfDay = Duration.ofHours(23)
                                     .plusMinutes(59)
                                     .plusSeconds(59);
      parametersMap.put(dateToParameter, filters.getDateTo().toInstant().plus(addEndOfDay).toEpochMilli());
    }
  }

  /**
   * Add predicate and parameter excluded ids.
   *
   * @param fieldValue the field value
   * @param criteriaBuilder the criteria builder
   * @param predicates the predicates
   * @param run the run
   * @param parametersMap the parameters map
   */
  public static void addPredicateAndParameterExcludedIds(Set<Long> fieldValue, CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
      Join<LinkRow, RunRow> run, Map<ParameterExpression<?>, Object> parametersMap) {
    if (!(fieldValue == null || fieldValue.isEmpty())) {
      ParameterExpression<Set> excludeCheckIdsParameter = criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_CHECK_IDS);
      predicates.add(criteriaBuilder.not(run.get(FieldNames.RUN_ID_DB).in(excludeCheckIdsParameter)));
      parametersMap.put(excludeCheckIdsParameter, fieldValue);
    }
  }

  /**
   * Percent links in operation boolean.
   *
   * @param filters the filters
   * @param tuple the tuple
   * @return the boolean
   */
  public static boolean percentLinksInOperation(FieldFilters filters, Tuple tuple) {
    return ((filters.getPercentLinksInOperationFrom() == null)
        || ((long) tuple.get(FieldNames.ERROR_LINKS_DB) * 100 / (long) tuple.get(FieldNames.TOTAL_LINKS_DB))
        >= filters.getPercentLinksInOperationFrom())
        && ((filters.getPercentLinksInOperationTo() == null)
        || ((long) tuple.get(FieldNames.ERROR_LINKS_DB) * 100 / (long) tuple.get(FieldNames.TOTAL_LINKS_DB))
        <= filters.getPercentLinksInOperationTo());
  }

  /**
   * Add predicate and parameter.
   *
   * @param fieldValue the field value
   * @param criteriaBuilder the criteria builder
   * @param predicates the predicates
   * @param dataset the dataset
   * @param parametersMap the parameters map
   * @param fieldName the field name
   */
  public static void addPredicateAndParameter(Set<String> fieldValue, CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
      Join<RunRow, DatasetRow> dataset, Map<ParameterExpression<?>, Object> parametersMap, String fieldName) {
    if (!(fieldValue == null || fieldValue.isEmpty())) {
      ParameterExpression<Set> parameter = criteriaBuilder.parameter(Set.class, fieldName+"Parameter");
      predicates.add(dataset.get(fieldName).in(parameter));
      parametersMap.put(parameter, fieldValue);
    }
  }

  /**
   * Helper holder for parts used in criteria building.
   */
  public record QueryParts(CriteriaQuery<Tuple> criteriaQuery,
                                   Root<LinkRow> link,
                                   Join<LinkRow, RunRow> run,
                                   Join<RunRow, DatasetRow> dataset,
                                   Join<RunRow, BatchRow> batch,
                                   List<Predicate> predicates,
                                   Map<ParameterExpression<?>, Object> parametersMap) {}

  /**
   * Build check runs query parts
   * This method builds the common parts of the criteria query for fetching check runs,
   * including the root, joins, and initial structures for predicates and parameters.
   *
   * @param criteriaBuilder the criteria builder
   * @return the query parts
   */
  public static QueryParts buildCheckRunsQueryParts(CriteriaBuilder criteriaBuilder) {
    CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();
    Root<LinkRow> link = criteriaQuery.from(LinkRow.class);
    Join<LinkRow, RunRow> run = link.join("run", JoinType.INNER);
    Join<RunRow, DatasetRow> dataset = run.join("dataset", JoinType.INNER);
    Join<RunRow, BatchRow> batch = run.join("batch", JoinType.INNER);
    List<Predicate> predicates = new ArrayList<>();
    Map<ParameterExpression<?>, Object> parametersMap = new HashMap<>();
    return new QueryParts(criteriaQuery, link, run, dataset, batch, predicates, parametersMap);
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
