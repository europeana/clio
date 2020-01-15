package eu.europeana.clio.common.model;

import java.time.Instant;

public class Link {

  private final long linkId;
  private final String recordId;
  private final LinkType linkType;
  private final String linkUrl;
  private final String server;
  private final String error;
  private final Instant checkingTime;

  public Link(long linkId, String recordId, LinkType linkType, String linkUrl,
          String server, String error, Instant checkingTime) {
    this.linkId = linkId;
    this.recordId = recordId;
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
}
