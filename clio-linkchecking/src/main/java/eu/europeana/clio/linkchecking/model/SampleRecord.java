package eu.europeana.clio.linkchecking.model;

import eu.europeana.clio.common.model.LinkType;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a sample record for a dataset.
 */
public class SampleRecord {

  private final String recordId;
  private final Instant recordLastIndexTime;
  private final Map<LinkType, Set<String>> links;

  /**
   * Constructor.
   *
   * @param recordId The Europeana record ID.
   * @param recordLastIndexTime The last time this record was indexed.
   * @param links The links in the record, sorted by link type.
   */
  public SampleRecord(String recordId, Instant recordLastIndexTime,
          Map<LinkType, Set<String>> links) {
    this.recordId = recordId;
    this.recordLastIndexTime = recordLastIndexTime;
    this.links = new EnumMap<>(links);
  }

  public String getRecordId() {
    return recordId;
  }

  public Instant getRecordLastIndexTime() {
    return recordLastIndexTime;
  }

  public Map<LinkType, Set<String>> getLinks() {
    return Collections.unmodifiableMap(links);
  }
}
