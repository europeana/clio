package eu.europeana.clio.common.model;

/**
 * The different link types that are checked by Clio.
 */
public enum LinkType {

  /**
   * edm:isShownAt links.
   */
  IS_SHOWN_AT("edm:isShownAt"),

  /**
   * edm:isShownBy links.
   */
  IS_SHOWN_BY("edm:isShownBy");

  private final String humanReadableName;

  LinkType(String humanReadableName) {
    this.humanReadableName = humanReadableName;
  }

  public String getHumanReadableName() {
    return humanReadableName;
  }
}
