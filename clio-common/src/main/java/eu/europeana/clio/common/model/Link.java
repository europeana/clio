package eu.europeana.clio.common.model;

import java.time.Instant;

/**
 * This class represents a link (to be checked once as part of a run).
 */
public class Link {

  private final long linkId;
  private final String recordId;
  private final Instant recordLastIndexTime;
  private final String recordEdmType;
  private final String recordContentTier;
  private final String recordMetadataTier;
  private final LinkType linkType;
  private final String linkUrl;
  private final String server;
  private final String error;
  private final Instant checkingTime;

  /**
   * Constructor.
   *
   * @param linkId The ID of the link.
   * @param recordId The Europeana record ID in which this link is present.
   * @param recordLastIndexTime The last time this record was indexed.
   * @param recordEdmType The edm:type of the record.
   * @param recordContentTier The content tier of the record.
   * @param recordMetadataTier The metadata tier of the record.
   * @param linkType The type of the link reference in the record.
   * @param linkUrl The actual link.
   * @param server The server of the link. Can be null if the server could not be computed.
   * @param error The error that occurred during link checking in Clio.
   * @param checkingTime The time that Clio checked the link.
   */
  public Link(long linkId, String recordId, Instant recordLastIndexTime, String recordEdmType,
          String recordContentTier, String recordMetadataTier, LinkType linkType,
          String linkUrl, String server, String error, Instant checkingTime) {
    this.linkId = linkId;
    this.recordId = recordId;
    this.recordLastIndexTime = recordLastIndexTime;
    this.recordEdmType = recordEdmType;
    this.recordContentTier = recordContentTier;
    this.recordMetadataTier = recordMetadataTier;
    this.linkType = linkType;
    this.linkUrl = linkUrl;
    this.server = server;
    this.error = error;
    this.checkingTime = checkingTime;
  }

  public long getLinkId() {
    return linkId;
  }

  public String getRecordId() {
    return recordId;
  }

  public Instant getRecordLastIndexTime() {
    return recordLastIndexTime;
  }

  public String getRecordEdmType() {
    return recordEdmType;
  }

  public String getRecordContentTier() {
    return recordContentTier;
  }

  public String getRecordMetadataTier() {
    return recordMetadataTier;
  }

  public LinkType getLinkType() {
    return linkType;
  }

  public String getLinkUrl() {
    return linkUrl;
  }

  public String getServer() {
    return server;
  }

  public String getError() {
    return error;
  }

  public Instant getCheckingTime() {
    return checkingTime;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    final Link that = (Link) obj;
    return this.linkId == that.linkId;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(linkId);
  }
}
