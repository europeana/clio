package eu.europeana.clio.common.model;

/**
 * This represents a dataset as presisted in the Clio database.
 */
public class Dataset {

  private final String datasetId;
  private final String name;
  private final int size;
  private final String provider;
  private final String dataProvider;

  /**
   * Constructor.
   *
   * @param datasetId The (Metis) dataset ID.
   * @param name The name of the dataset.
   * @param size The size of the dataset (of the last indexing to publish) if known. Otherwise, -1.
   * @param provider The provider of the dataset.
   * @param dataProvider The data provider of the dataset.
   */
  public Dataset(String datasetId, String name, int size, String provider,
          String dataProvider) {
    this.datasetId = datasetId;
    this.name = name;
    this.size = size;
    this.provider = provider;
    this.dataProvider = dataProvider;
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
}
