package eu.europeana.clio.common.persistence.model;

import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.apache.commons.lang3.StringUtils;

/**
 * This represents the persistent form of a (Clio) dataset.
 */
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

  @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
  private String name;

  @Column(name = "size")
  private Integer size;

  @Column(name = "last_index_time")
  private Long lastIndexTime;

  @Column(name = "provider", length = MAX_PROVIDER_LENGTH)
  private String provider;

  @Column(name = "data_provider", length = MAX_DATA_PROVIDER_LENGTH)
  private String dataProvider;

  /**
   * Constructor for the use of JPA. Don't use from code.
   */
  protected DatasetRow() {
  }

  /**
   * Constructor.
   *
   * @param datasetId The (Metis) dataset ID.
   */
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

  public Integer getSize() {
    return size;
  }

  public Instant getLastIndexTime() {
    return Optional.ofNullable(lastIndexTime).map(Instant::ofEpochMilli).orElse(null);
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

  public void setSize(Integer size) {
    this.size = size;
  }

  public void setLastIndexTime(Instant lastIndexTime) {
    this.lastIndexTime = Optional.ofNullable(lastIndexTime).map(Instant::toEpochMilli).orElse(null);
  }

  public void setProvider(String provider) {
    this.provider = StringUtils.truncate(provider, MAX_PROVIDER_LENGTH);
  }

  public void setDataProvider(String dataProvider) {
    this.dataProvider = StringUtils.truncate(dataProvider, MAX_DATA_PROVIDER_LENGTH);
  }
}
