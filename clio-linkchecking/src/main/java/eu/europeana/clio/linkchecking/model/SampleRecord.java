package eu.europeana.clio.linkchecking.model;

import eu.europeana.clio.common.model.LinkType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class SampleRecord {

  private final String recordId;
  private final Map<LinkType, Set<String>> links;

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
