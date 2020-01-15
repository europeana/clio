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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "link", uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "link_url"}),
        indexes = @Index(columnList = "server"))
@NamedQueries({
        @NamedQuery(name = LinkRow.GET_UNCHECKED_LINKS, query =
                "SELECT l FROM LinkRow AS l WHERE l.checkingTime IS NULL"),
        @NamedQuery(name = LinkRow.GET_UNCHECKED_LINKS_BY_URL, query =
                "SELECT l FROM LinkRow AS l WHERE l.linkUrl = :" + LinkRow.LINK_URL_PARAMETER
                        + " AND l.checkingTime IS NULL")})
public class LinkRow {

  public static final String GET_UNCHECKED_LINKS = "getUncheckedLinks";
  public static final String GET_UNCHECKED_LINKS_BY_URL = "getUncheckedLinksByUrl";
  public static final String LINK_URL_PARAMETER = "linkUrl";

  private static final int MAX_RECORD_ID_LENGTH = 256;
  private static final int MAX_LINK_URL_LENGTH = 256;
  private static final int MAX_SERVER_LENGTH = 128;
  private static final int MAX_ERROR_LENGTH = 512;

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

  @Column(name = "server", updatable = false, length = MAX_SERVER_LENGTH)
  private String server;

  @Column(name = "error", updatable = false, length = MAX_ERROR_LENGTH)
  private String error;

  @Column(name = "checking_time")
  private Long checkingTime;

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

  public void setError(String error) {
    this.error = StringUtils.truncate(error, MAX_ERROR_LENGTH);
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

  public String getError() {
    return error;
  }

  public Long getCheckingTime() {
    return checkingTime;
  }
}
