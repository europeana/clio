package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.ClioFilterField;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.DatasetSummary;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.util.CollectionUtils;

/**
 * Data access object for (Clio) datasets.
 */
public class DatasetDao {

  private final HibernateSessionUtils hibernateSessionUtils;

  /**
   * Constructor.
   *
   * @param sessionFactory The connection to the Clio persistence. Should be connected. This object does not close the
   * connection.
   */
  public DatasetDao(SessionFactory sessionFactory) {
    this.hibernateSessionUtils = new HibernateSessionUtils(sessionFactory);
  }

  /**
   * Create (i.e. persist, if a dataset with the given ID does not yet exist) or update (otherwise) a dataset.
   *
   * @param dataset The dataset to persist or update.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public void createOrUpdateDataset(Dataset dataset) throws PersistenceException {
    hibernateSessionUtils.performInTransaction(session -> {
      final DatasetRow existingRow = session.find(DatasetRow.class, dataset.getDatasetId());
      if (existingRow == null) {
        final DatasetRow newRow = new DatasetRow(dataset.getDatasetId());
        setPropertiesToRow(dataset, newRow);
        session.persist(newRow);
      } else {
        setPropertiesToRow(dataset, existingRow);
      }
      return null;
    });
  }

  private void setPropertiesToRow(Dataset dataset, DatasetRow row) {
    row.setName(dataset.getName());
    row.setSize(dataset.getSize());
    row.setLastIndexTime(dataset.getLastIndexTime());
    row.setProvider(dataset.getProvider());
    row.setDataProvider(dataset.getDataProvider());
  }

  static Dataset convert(DatasetRow row) {
    return new Dataset(row.getDatasetId(), row.getName(), row.getSize(), row.getLastIndexTime(),
        row.getProvider(), row.getDataProvider());
  }

  public static final double HUNDRED = 100.0D;

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
      List<Predicate> predicates,
      Join<DatasetRow, RunRow> dataset, Map<ParameterExpression<?>, Object> parametersMap) {
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

  public static void addPredicateAndParameterLastThreeMonths(CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
      Root<LinkRow> link, Map<ParameterExpression<?>, Object> parametersMap) {
    LocalDate filterPeriodWindow = LocalDate.now(ZoneOffset.UTC);
    long startingWindowTime = filterPeriodWindow.minusDays(90L).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
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
      List<Predicate> predicates,
      Join<DatasetRow, RunRow> dataset, Map<ParameterExpression<?>, Object> parametersMap) {

    if (!CollectionUtils.isEmpty(fieldValue)) {
      ParameterExpression<Set> excludeCheckIdsParameter = criteriaBuilder.parameter(Set.class, FieldNames.EXCLUDED_DATASET_ID);
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
      List<Predicate> predicates,
      Expression<Integer> expressionPercentage, Map<ParameterExpression<?>, Object> parametersMap) {
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
   * @param criteriaBuilder the criteria builder
   * @param clazz the result class
   * @param filters the field filters to apply
   * @return common query parts with predicates already applied
   */
  public static <T> CommonDatasetQueryParts<T> buildCommonDatasetQueryWithPredicates(
      CriteriaBuilder criteriaBuilder, Class<T> clazz, FieldFilters filters) {
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

    Expression<Integer> percentLinksInOperation = criteriaBuilder.diff(HUNDRED,
        criteriaBuilder.prod(
            criteriaBuilder.<Double>selectCase()
                           .when(criteriaBuilder.equal(totalLinks, 0D), 0D)
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
    addPredicateAndParameterExcludedIds(filters.getExcludedDatasetId(), criteriaBuilder,
        wherePredicates, dataset, parametersMap);
    addPredicateAndParameterLastThreeMonths(criteriaBuilder, wherePredicates, link, parametersMap);
    addPredicateAndParameterDateRange(filters, criteriaBuilder, wherePredicates,
        dataset, parametersMap);
    addPredicatePercentLinksInOperation(filters, criteriaBuilder, havingPredicates,
        percentLinksInOperation, parametersMap);

    return new CommonDatasetQueryParts<>(
        criteriaQuery,
        link,
        run,
        dataset,
        errorsLinks,
        totalLinks,
        percentLinksInOperation,
        wherePredicates,
        havingPredicates,
        parametersMap
    );
  }


  /**
   * Find datasets summary filter options field filters.
   *
   * @param filters the filters
   * @return the field filters
   * @throws PersistenceException the persistence exception
   */
  public FieldFilters findDatasetsSummaryFilterOptions(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performInSession(session -> {
      TypedQuery<DatasetSummary> query = getDatasetSummaryTypedQuery(filters, session);
      List<DatasetSummary> datasetSummaries = query.getResultStream().toList();
      Map<ClioFilterField, Set<String>> result = new EnumMap<>(ClioFilterField.class);
      ClioFilterField
          .getValueFields()
          .forEach(fieldName -> {
            Set<String> stringSet = switch (fieldName) {
              case DATASET_NAME -> datasetSummaries.stream()
                                                   .map(DatasetSummary::datasetName)
                                                   .filter(value -> value != null && !value.isEmpty())
                                                   .collect(Collectors.toSet());
              case DATASET_ID -> datasetSummaries.stream()
                                                 .map(DatasetSummary::datasetId)
                                                 .filter(value -> value != null && !value.isEmpty())
                                                 .collect(Collectors.toSet());
              case PROVIDER -> datasetSummaries.stream()
                                               .map(DatasetSummary::provider)
                                               .filter(value -> value != null && !value.isEmpty())
                                               .collect(Collectors.toSet());
              case DATA_PROVIDER -> datasetSummaries.stream()
                                                    .map(DatasetSummary::dataProvider)
                                                    .filter(value -> value != null && !value.isEmpty())
                                                    .collect(Collectors.toSet());
              default -> Set.of();
            };
            result.put(fieldName, stringSet);
          });

      return new FieldFilters(new TreeSet<>(result.get(ClioFilterField.PROVIDER)),
          new TreeSet<>(result.get(ClioFilterField.DATA_PROVIDER)),
          new TreeSet<>(result.get(ClioFilterField.DATASET_ID)),
          new TreeSet<>(result.get(ClioFilterField.DATASET_NAME)),
          filters.getExcludedDatasetId(),
          filters.getDateFrom(),
          filters.getDateTo(),
          filters.getPercentLinksInOperationFrom(),
          filters.getPercentLinksInOperationTo(),
          filters.getOffset(),
          filters.getLimit(),
          filters.isMoreAvailable());
    });
  }

  /**
   * Finds datasets summary.
   *
   * @param filters the filters
   * @return the check runs
   * @throws PersistenceException the persistence exception
   */
  public List<DatasetSummary> findDatasetsSummary(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performInSession(session -> {
      TypedQuery<DatasetSummary> query = getDatasetSummaryTypedQuery(filters, session);
      List<DatasetSummary> tempDatasetSummaries = query.setFirstResult(filters.getOffset())
                                                       .setMaxResults(filters.getLimit() + 1)
                                                       .getResultList();

      return pagingHasMoreAvailable(filters, tempDatasetSummaries);
    });
  }

  private List<DatasetSummary> pagingHasMoreAvailable(FieldFilters filters, List<DatasetSummary> tempDatasetSummaries) {
    List<DatasetSummary> datasetSummaries;
    if ((long) tempDatasetSummaries.size() < filters.getLimit()) {
      filters.setMoreAvailable(false);
      datasetSummaries = tempDatasetSummaries;
    } else {
      filters.setMoreAvailable(true);
      datasetSummaries = tempDatasetSummaries.subList(0, filters.getLimit());
    }
    return datasetSummaries;
  }

  public static TypedQuery<DatasetSummary> getDatasetSummaryTypedQuery(FieldFilters filters, Session session) {
    CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
    CommonDatasetQueryParts<DatasetSummary> queryParts = buildCommonDatasetQueryWithPredicates(
        criteriaBuilder, DatasetSummary.class, filters);

    CriteriaQuery<DatasetSummary> criteriaQuery = queryParts.criteriaQuery();

    // select
    criteriaQuery.select(criteriaBuilder.construct(
        DatasetSummary.class,
        queryParts.dataset().get(FieldNames.DATASET_ID_DB),
        queryParts.dataset().get(FieldNames.DATASET_NAME_DB),
        queryParts.dataset().get(FieldNames.DATASET_SIZE),
        queryParts.dataset().get(FieldNames.DATASET_LAST_INDEX),
        queryParts.dataset().get(FieldNames.DATA_PROVIDER),
        queryParts.dataset().get(FieldNames.PROVIDER),
        queryParts.percentLinksInOperation().alias(FieldNames.PERCENT_LINKS_IN_OPERATION_DB)
    ));

    // where & having
    criteriaQuery.where(criteriaBuilder.and(queryParts.wherePredicates()));
    criteriaQuery.having(queryParts.havingPredicates());

    // order by (specific to LinkDao)
    criteriaQuery.orderBy(criteriaBuilder.asc(queryParts.dataset().get(FieldNames.DATASET_ID_DB)));

    // group by
    criteriaQuery.groupBy(
        queryParts.dataset().get(FieldNames.DATASET_ID_DB),
        queryParts.dataset().get(FieldNames.DATASET_NAME_DB),
        queryParts.dataset().get(FieldNames.DATASET_SIZE),
        queryParts.dataset().get(FieldNames.DATASET_LAST_INDEX),
        queryParts.dataset().get(FieldNames.PROVIDER),
        queryParts.dataset().get(FieldNames.DATA_PROVIDER)
    );

    // execute query
    TypedQuery<DatasetSummary> query = session.createQuery(criteriaQuery);
    queryParts.parametersMap().forEach((key, value) -> query.setParameter(key.getName(), value));
    return query;
  }

  /**
   * Represents the common components of a run summary criteria query.
   *
   * @param <T> the type parameter e.g., a RunSummary
   * @param criteriaQuery the criteria query
   * @param link the link
   * @param run the run
   * @param dataset the dataset
   * @param errorsLinks the error's link
   * @param totalLinks the total links
   * @param percentLinksInOperation the percent links in operation
   * @param wherePredicates the where predicates
   * @param havingPredicates the having predicates
   * @param parametersMap the parameter map
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
}
