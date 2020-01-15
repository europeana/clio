package eu.europeana.clio.linkchecking.model;

import eu.europeana.clio.common.model.LinkType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a sample record for a dataset.
 */
public class SampleRecord {

  private final String recordId;
  private final Map<LinkType, Set<String>> links;

  /**
   * Constructor.
   *
   * @param recordId The Europeana record ID.
   * @param links The links in the record, sorted by link type.
   */
  public SampleRecord(String recordId, Map<LinkType, Set<String>> links) {
    this.recordId = recordId;
    this.links = new EnumMap<>(links);
  }

  public String getRecordId() {
    return recordId;
  }

  public Map<LinkType, Set<String>> getLinks() {
    return Collections.unmodifiableMap(links);
  }
}
