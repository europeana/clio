package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.persistence.ConnectionProvider;
import eu.europeana.clio.common.persistence.model.DatasetRow;

public class DatasetDao {

  private final ConnectionProvider connectionProvider;

  public DatasetDao(ConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  public void createOrUpdateDataset(Dataset dataset) throws PersistenceException {
    connectionProvider.performInTransaction(session -> {
      final DatasetRow existingRow = session.get(DatasetRow.class, dataset.getDatasetId());
      if (existingRow != null) {
        setPropertiesToRow(dataset, existingRow);
      } else {
        final DatasetRow newRow = new DatasetRow(dataset.getDatasetId());
        setPropertiesToRow(dataset, newRow);
        session.persist(newRow);
      }
      return null;
    });
  }

  private void setPropertiesToRow(Dataset dataset, DatasetRow row) {
    row.setName(dataset.getName());
    row.setSize(dataset.getSize());
    row.setProvider(dataset.getProvider());
    row.setDataProvider(dataset.getDataProvider());
  }

  public Dataset getDataset(String datasetId) throws PersistenceException {
    final DatasetRow row = connectionProvider.performInSession(session -> session.get(DatasetRow.class, datasetId));
    if (row == null) {
      return null;
    }
    return new Dataset(row.getDatasetId(), row.getName(), row.getSize(), row.getProvider(),
            row.getDataProvider());
  }
}
