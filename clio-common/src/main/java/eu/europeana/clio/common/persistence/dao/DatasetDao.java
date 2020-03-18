package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.model.DatasetRow;

/**
 * Data access object for (Clio) datasets.
 */
public class DatasetDao {

  private final ClioPersistenceConnection persistenceConnection;

  /**
   * Constructor.
   *
   * @param persistenceConnection The connection to the Clio persistence. Should be connected. This
   * object does not close the connection.
   */
  public DatasetDao(ClioPersistenceConnection persistenceConnection) {
    this.persistenceConnection = persistenceConnection;
  }

  /**
   * Create (i.e. persist, if a dataset with the given ID does not yet exist) or update (otherwise)
   * a dataset.
   *
   * @param dataset The dataset to persist or update.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public void createOrUpdateDataset(Dataset dataset) throws PersistenceException {
    persistenceConnection.performInTransaction(session -> {
      final DatasetRow existingRow = session.get(DatasetRow.class, dataset.getDatasetId());
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
}
