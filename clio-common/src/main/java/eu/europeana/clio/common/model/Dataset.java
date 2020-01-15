package eu.europeana.clio.common.model;

public class Dataset {

  private final String datasetId;
  private final String name;
  private final int size;
  private final String provider;
  private final String dataProvider;

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
