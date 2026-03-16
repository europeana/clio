package eu.europeana.clio.common.persistence.dao;

import static java.lang.String.format;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.CheckRecord;
import eu.europeana.clio.common.model.FieldFilters;
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
import java.util.HashMap;
import java.util.List;
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

  public List<CheckRecord> getCheckRuns(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performInSession(session -> {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();

      Root<LinkRow> link = criteriaQuery.from(LinkRow.class);

      // joins
      Join<LinkRow, RunRow> run = link.join("run", JoinType.INNER);
      Join<RunRow, DatasetRow> dataset = run.join("dataset", JoinType.INNER);
      Join<RunRow, BatchRow> batch = run.join("batch", JoinType.INNER);

      List<Predicate> predicates = new ArrayList<>();
      HashMap<ParameterExpression<?>, Object> parametersMap = new HashMap<>();

      // predicates
      if (!(filters.getProvider() == null || filters.getProvider().isEmpty())) {
        ParameterExpression<Set> providersParameter = criteriaBuilder.parameter(Set.class, "providers");
        predicates.add(dataset.get("provider").in(providersParameter));
        parametersMap.put(providersParameter, filters.getProvider());
      }

      if (!(filters.getDataProvider() == null || filters.getDataProvider().isEmpty())) {
        ParameterExpression<Set> dataProvidersParameter = criteriaBuilder.parameter(Set.class, "dataProviders");
        predicates.add(dataset.get("dataProvider").in(dataProvidersParameter));
        parametersMap.put(dataProvidersParameter, filters.getDataProvider());
      }

      if (!(filters.getDatasetId() == null || filters.getDatasetId().isEmpty())) {
        ParameterExpression<Set> datasetIdParameter = criteriaBuilder.parameter(Set.class, "datasetIds");
        predicates.add(dataset.get("datasetId").in(datasetIdParameter));
        parametersMap.put(datasetIdParameter, filters.getDatasetId());
      }

      if (!(filters.getDatasetName() == null || filters.getDatasetName().isEmpty())) {
        ParameterExpression<Set> datasetNameParameter = criteriaBuilder.parameter(Set.class, "datasetName");
        predicates.add(dataset.get("name").in(datasetNameParameter));
        parametersMap.put(datasetNameParameter, filters.getDatasetName());
      }

      if (!(filters.getExcludedCheckIds() == null || filters.getExcludedCheckIds().isEmpty())) {
        ParameterExpression<Set> excludeCheckIdsParameter = criteriaBuilder.parameter(Set.class, "excludedCheckIds");
        predicates.add(criteriaBuilder.not(run.get("runId").in(excludeCheckIdsParameter)));
        parametersMap.put(excludeCheckIdsParameter, filters.getExcludedCheckIds());
      }

      if (filters.getDateFrom() != null) {
        ParameterExpression<Long> dateFromParameter = criteriaBuilder.parameter(Long.class, "startingTime");
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(run.get("startingTime"), dateFromParameter));
        parametersMap.put(dateFromParameter, filters.getDateFrom().toInstant().toEpochMilli());
      }

      if (filters.getDateTo() != null) {
        ParameterExpression<Long> dateToParameter = criteriaBuilder.parameter(Long.class, "endTime");
        predicates.add(criteriaBuilder.lessThanOrEqualTo(run.get("startingTime"), dateToParameter));
        Duration addEndOfDay = Duration.ofHours(23)
                                       .plusMinutes(59)
                                       .plusSeconds(59);
        parametersMap.put(dateToParameter, filters.getDateTo().toInstant().plus(addEndOfDay).toEpochMilli());
      }

      // OR combination
      Predicate whereClause = criteriaBuilder.and(predicates);

      // aggregations
      Expression<Long> errorsLinks = criteriaBuilder.count(link.get("error"));
      Expression<Long> totalLinks = criteriaBuilder.count(run.get("runId"));
      Expression<Long> startingTime = criteriaBuilder.min(run.get("startingTime"));

      // select
      criteriaQuery.select(criteriaBuilder.tuple(
          run.get("runId"),
          dataset,
          startingTime.alias("startingTime"),
          errorsLinks.alias("errorsLinks"),
          totalLinks.alias("totalLinks")
      ));

      // where
      criteriaQuery.where(whereClause);

      // group by
      criteriaQuery.groupBy(
          batch.get("batchId"),
          dataset.get("datasetId"),
          run.get("runId")
      );

      // create query
      TypedQuery<Tuple> query = session.createQuery(criteriaQuery);

      // set value to parameters
      parametersMap.forEach((key, value) -> query.setParameter(key.getName(), value));

      return query
          .getResultStream()
          .filter(tuple ->
              (filters.getPercentLinksInOperationFrom() == null)
                  || ((long) tuple.get("errorsLinks") * 100 / (long) tuple.get("totalLinks"))
                  >= filters.getPercentLinksInOperationFrom()
                  && (filters.getPercentLinksInOperationTo() == null)
                  || ((long) tuple.get("errorsLinks") * 100 / (long) tuple.get("totalLinks"))
                  <= filters.getPercentLinksInOperationTo())
          .map(tuple -> {
            DatasetRow datasetRow = tuple.get(1, DatasetRow.class);
            var percentResult = (int) ((long) tuple.get("errorsLinks") /  ((long) tuple.get("totalLinks")) *100);
            return new CheckRecord((long) tuple.get(0),
                Instant.ofEpochMilli((long) tuple.get("startingTime")),
                datasetRow.getDatasetId(),
                datasetRow.getName(),
                datasetRow.getSize(),
                datasetRow.getLastIndexTime(),
                datasetRow.getProvider(),
                datasetRow.getDataProvider(),
                percentResult);
          }).toList();
    });
  }

  static Run convert(RunRow row) {
    return new Run(row.getRunId(), row.getStartingTime(), DatasetDao.convert(row.getDataset()));
  }

}
