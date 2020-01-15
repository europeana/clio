package eu.europeana.clio.common.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "link", uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "link_url"}),
        indexes = @Index(columnList = "server"))
public class LinkRow {

  private static final int MAX_RECORD_ID_LENGTH = 256;
  private static final int MAX_LINK_URL_LENGTH = 256;
  private static final int MAX_SERVER_LENGTH = 128;
  private static final int MAX_RESULT_LENGTH = 512;

  public enum LinkType {IS_SHOWN_AT, IS_SHOWN_BY}

  @Id
  @Column(name = "link_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long linkId;

  @ManyToOne
  @JoinColumn(name = "run_id", referencedColumnName = "run_id", updatable = false, nullable = false)
  private RunRow run;

  @Column(name = "record_id", nullable = false, updatable = false, length = MAX_RECORD_ID_LENGTH)
  private String recordId;

  @Column(name = "link_type", nullable = false, updatable = false, length = 11)
  @Enumerated(EnumType.STRING)
  private LinkType linkType;

  @Column(name = "link_url", nullable = false, updatable = false, length = MAX_LINK_URL_LENGTH)
  private String linkUrl;

  @Column(name = "server", nullable = false, updatable = false, length = MAX_SERVER_LENGTH)
  private String server;

  @Column(name = "result", updatable = false, length = MAX_RESULT_LENGTH)
  private String result;

  @Column(name = "checking_time")
  private long checkingTime;

  LinkRow() {
  }

  public LinkRow(RunRow run, String recordId, LinkType linkType, String linkUrl, String server) {
    if (recordId.length() > MAX_RECORD_ID_LENGTH) {
      throw new IllegalArgumentException("Record ID is too long: " + recordId);
    }
    if (linkUrl.length() > MAX_LINK_URL_LENGTH) {
      throw new IllegalArgumentException("Link URL is too long: " + linkUrl);
    }
    if (server.length() > MAX_SERVER_LENGTH) {
      throw new IllegalArgumentException("Server is too long: " + server);
    }
    this.run = run;
    this.recordId = recordId;
    this.linkType = linkType;
    this.linkUrl = linkUrl;
    this.server = server;
  }

  public void setResult(String result) {
    this.result = StringUtils.truncate(result, MAX_RESULT_LENGTH);
  }

  public void setCheckingTime(long checkingTime) {
    this.checkingTime = checkingTime;
  }

  public long getLinkId() {
    return linkId;
  }

  public RunRow getRun() {
    return run;
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

  public String getResult() {
    return result;
  }

  public long getCheckingTime() {
    return checkingTime;
  }
}
