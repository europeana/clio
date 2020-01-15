package eu.europeana.clio.common.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "dataset")
public class DatasetRow {

  private static final int MAX_DATASET_ID_LENGTH = 16;
  private static final int MAX_NAME_LENGTH = 64;
  private static final int MAX_PROVIDER_LENGTH = 64;
  private static final int MAX_DATA_PROVIDER_LENGTH = 64;

  @Id
  @Column(name = "dataset_id", length = MAX_DATASET_ID_LENGTH)
  private String datasetId;

  @Column(name = "name", length = MAX_NAME_LENGTH, nullable = false)
  private String name;

  @Column(name = "size", nullable = false)
  private int size;

  @Column(name = "provider", length = MAX_PROVIDER_LENGTH)
  private String provider;

  @Column(name = "data_provider", length = MAX_DATA_PROVIDER_LENGTH)
  private String dataProvider;

  DatasetRow() {
  }

  public DatasetRow(String datasetId) {
    if (datasetId.length() > MAX_DATASET_ID_LENGTH) {
      throw new IllegalArgumentException("Dataset ID is too long: " + datasetId);
    }
    this.datasetId = datasetId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public String getName() {
    return name;
  }

  public int getSize() {
    return size;
  }

  public String getProvider() {
    return provider;
  }

  public String getDataProvider() {
    return dataProvider;
  }

  public void setName(String name) {
    this.name = StringUtils.truncate(name, MAX_NAME_LENGTH);
  }

  public void setSize(int size) {
    this.size = size;
  }

  public void setProvider(String provider) {
    this.provider = StringUtils.truncate(provider, MAX_PROVIDER_LENGTH);
  }

  public void setDataProvider(String dataProvider) {
    this.dataProvider = StringUtils.truncate(dataProvider, MAX_DATA_PROVIDER_LENGTH);
  }
}
