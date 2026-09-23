package eu.europeana.clio.common.persistence.dao;

import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.getDatasetCheckSummaryTypedQuery;
import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.getDatasetSummaryTypedQuery;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.ClioFilterField;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.DatasetCheckSummary;
import eu.europeana.clio.common.model.DatasetSummary;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.PagedDatasetResult;
import eu.europeana.clio.common.model.Pagination;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import jakarta.persistence.TypedQuery;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;

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

  /**
   * Convert dataset.
   *
   * @param row the row
   * @return the dataset
   */
  static Dataset convert(DatasetRow row) {
    return new Dataset(row.getDatasetId(), row.getName(), row.getSize(), row.getLastIndexTime(), row.getProvider(),
        row.getDataProvider());
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
      ClioFilterField.getValueFields().forEach(fieldName -> {
        Set<String> stringSet = switch (fieldName) {
          case DATASET_NAME ->
              datasetSummaries.stream().map(DatasetSummary::datasetName).filter(value -> value != null && !value.isEmpty())
                              .collect(Collectors.toSet());
          case DATASET_ID ->
              datasetSummaries.stream().map(DatasetSummary::datasetId).filter(value -> value != null && !value.isEmpty())
                              .collect(Collectors.toSet());
          case PROVIDER ->
              datasetSummaries.stream().map(DatasetSummary::provider).filter(value -> value != null && !value.isEmpty())
                              .collect(Collectors.toSet());
          case DATA_PROVIDER ->
              datasetSummaries.stream().map(DatasetSummary::dataProvider).filter(value -> value != null && !value.isEmpty())
                              .collect(Collectors.toSet());
          default -> Set.of();
        };
        result.put(fieldName, stringSet);
      });

      return new FieldFilters(new TreeSet<>(result.get(ClioFilterField.PROVIDER)),
          new TreeSet<>(result.get(ClioFilterField.DATA_PROVIDER)), new TreeSet<>(result.get(ClioFilterField.DATASET_ID)),
          new TreeSet<>(result.get(ClioFilterField.DATASET_NAME)), filters.getExcludedId(), filters.getDateFrom(),
          filters.getDateTo(), filters.getPercentLinksInOperationFrom(), filters.getPercentLinksInOperationTo());
    });
  }

  /**
   * Finds datasets summary.
   *
   * @param filters the filters
   * @param pagination the pagination
   * @return the check runs
   * @throws PersistenceException the persistence exception
   */
  public PagedDatasetResult findDatasetsSummary(FieldFilters filters, Pagination pagination) throws PersistenceException {
    List<DatasetSummary> datasetSummaries = hibernateSessionUtils.performInSession(session -> {
      TypedQuery<DatasetSummary> query = getDatasetSummaryTypedQuery(filters, session);
      return query.setFirstResult(pagination.offset()).setMaxResults(pagination.limit() + 1).getResultList();
    });
    PagedDatasetResult pagedDatasetResult;
    if ((long) datasetSummaries.size() <= pagination.limit()) {
      pagedDatasetResult = new PagedDatasetResult(datasetSummaries,
          new Pagination(pagination.offset(), pagination.limit(), false));

    } else {
      pagedDatasetResult = new PagedDatasetResult(datasetSummaries.subList(0, pagination.limit()),
          new Pagination(pagination.offset(), pagination.limit(), true));
    }
    return pagedDatasetResult;
  }

  /**
   * Find dataset check summary list.
   *
   * @param filters the filters
   * @return the list
   * @throws PersistenceException the persistence exception
   */
  public List<DatasetCheckSummary> findDatasetCheckSummary(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performInSession(session -> {
      TypedQuery<DatasetCheckSummary> query = getDatasetCheckSummaryTypedQuery(filters, session);
      return query.getResultList();
    });
  }
}
