package eu.europeana.clio.common.model;

import java.time.Instant;
import java.util.Objects;

/**
 * This represents a dataset as presisted in the Clio database.
 */
public class Dataset {

  private final String datasetId;
  private final String name;
  private final Integer size;
  private final Instant lastIndexTime;
  private final String provider;
  private final String dataProvider;

  /**
   * Constructor.
   *
   * @param datasetId The (Metis) dataset ID.
   * @param name The name of the dataset.
   * @param size The size of the dataset (of the last indexing to publish) if known. Can be null.
   * @param lastIndexTime The last index time of the dataset if known. Can be null.
   * @param provider The provider of the dataset.
   * @param dataProvider The data provider of the dataset.
   */
  public Dataset(String datasetId, String name, Integer size, Instant lastIndexTime,
          String provider, String dataProvider) {
    this.datasetId = datasetId;
    this.name = name;
    this.size = size;
    this.lastIndexTime = lastIndexTime;
    this.provider = provider;
    this.dataProvider = dataProvider;
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
    return lastIndexTime;
  }

  public String getProvider() {
    return provider;
  }

  public String getDataProvider() {
    return dataProvider;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    final Dataset that = (Dataset) obj;
    return Objects.equals(this.datasetId, that.datasetId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.datasetId);
  }
}
