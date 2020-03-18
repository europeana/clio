package eu.europeana.clio.common.persistence.model;

import java.time.Instant;
import java.util.Optional;
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
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;

/**
 * This represents the persistent form of a link (to be checked once as part of a run).
 */
@Entity
@Table(name = "link",
        indexes = {@Index(columnList = "server"), @Index(columnList = "link_url")})
@NamedQuery(name = LinkRow.GET_UNCHECKED_LINKS, query = "SELECT l FROM LinkRow AS l WHERE l.checkingTime IS NULL")
@NamedQuery(name = LinkRow.GET_UNCHECKED_LINKS_BY_URL, query =
        "SELECT l FROM LinkRow AS l WHERE l.linkUrl = :" + LinkRow.LINK_URL_PARAMETER
                + " AND l.checkingTime IS NULL")
@NamedQuery(name = LinkRow.GET_BROKEN_LINKS_IN_LATEST_COMPLETED_RUNS, query = "SELECT l"
        + " FROM LinkRow l"
        + " WHERE l.error IS NOT NULL"
        + " AND l.run.startingTime = ("
        + "   SELECT MAX(r2.startingTime) FROM RunRow AS r2"
        + "   WHERE r2.dataset = l.run.dataset"
        + "   AND NOT EXISTS("
        + "     SELECT l2 FROM LinkRow l2 WHERE l2.run = r2 AND l2.checkingTime IS NULL"
        + "   )"
        + " )"
        + " ORDER BY l.run.dataset.datasetId ASC, l.recordId ASC, l.linkType ASC, l.linkUrl ASC")
public class LinkRow {

  public static final String GET_BROKEN_LINKS_IN_LATEST_COMPLETED_RUNS = "getBrokenLinksInLatestCompletedRuns";

  public static final String GET_UNCHECKED_LINKS = "getUncheckedLinks";
  public static final String GET_UNCHECKED_LINKS_BY_URL = "getUncheckedLinksByUrl";
  public static final String LINK_URL_PARAMETER = "linkUrl";

  private static final int MAX_RECORD_ID_LENGTH = 256;
  private static final int MAX_LINK_URL_LENGTH = 768;
  private static final int MAX_SERVER_LENGTH = 128;
  private static final int MAX_ERROR_LENGTH = 512;

  /**
   * This represents the persistent form of a link type.
   */
  public enum LinkType {
    /**
     * edm:isShownAt links.
     */
    IS_SHOWN_AT,

    /**
     * edm:isShownBy links.
     */
    IS_SHOWN_BY
  }

  @Id
  @Column(name = "link_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long linkId;

  @ManyToOne
  @JoinColumn(name = "run_id", referencedColumnName = "run_id", nullable = false, updatable = false)
  private RunRow run;

  @Column(name = "record_id", nullable = false, updatable = false, length = MAX_RECORD_ID_LENGTH)
  private String recordId;

  @Column(name = "record_last_index_time", nullable = false, updatable = false)
  private long recordLastIndexTime;

  @Column(name = "link_type", nullable = false, updatable = false, length = 11)
  @Enumerated(EnumType.STRING)
  private LinkType linkType;

  @Column(name = "link_url", nullable = false, updatable = false, length = MAX_LINK_URL_LENGTH)
  private String linkUrl;

  @Column(name = "server", updatable = false, length = MAX_SERVER_LENGTH)
  private String server;

  @Column(name = "error", length = MAX_ERROR_LENGTH)
  private String error;

  @Column(name = "checking_time")
  private Long checkingTime;

  /**
   * Constructor for the use of JPA. Don't use from code.
   */
  protected LinkRow() {
  }

  /**
   * Constructor. Creates a link in an unchecked state. If the string of either the record ID, the
   * link URL or the server is too long to fit in the field, this instance will be in a state of
   * being checked with errors.
   *
   * @param run The run to which this link belongs.
   * @param recordId The Europeana record ID in which this link is present.
   * @param recordLastIndexTime The last time this record was indexed.
   * @param linkType The type of the link reference in the record.
   * @param linkUrl The actual link.
   * @param server The server of the link. Can be null if the server could not be computed.
   */
  public LinkRow(RunRow run, String recordId, Instant recordLastIndexTime, LinkType linkType,
          String linkUrl, String server) {

    // Set basic properties
    this.run = run;
    this.recordId = StringUtils.truncate(recordId, MAX_RECORD_ID_LENGTH);
    this.recordLastIndexTime = recordLastIndexTime.toEpochMilli();
    this.linkType = linkType;
    this.linkUrl = StringUtils.truncate(linkUrl, MAX_LINK_URL_LENGTH);
    this.server = StringUtils.truncate(server, MAX_SERVER_LENGTH);

    // Test the length of certain properties
    final String error;
    if (recordId.length() > MAX_RECORD_ID_LENGTH) {
      error = "Record ID is too long: " + recordId;
    } else if (linkUrl.length() > MAX_LINK_URL_LENGTH) {
      error = "Link URL is too long: " + linkUrl;
    } else if (server == null) {
      error = "Server could not be determined for link: " + linkUrl;
    } else if (server.length() > MAX_SERVER_LENGTH) {
      error = "Server is too long: " + server;
    } else {
      error = null;
    }
    if (error != null) {
      setError(error);
      setCheckingTime(Instant.now());
    }
  }

  public void setError(String error) {
    this.error = StringUtils.truncate(error, MAX_ERROR_LENGTH);
  }

  public void setCheckingTime(Instant checkingTime) {
    this.checkingTime = checkingTime.toEpochMilli();
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

  public Instant getRecordLastIndexTime() {
    return Instant.ofEpochMilli(recordLastIndexTime);
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
    return Optional.ofNullable(checkingTime).map(Instant::ofEpochMilli).orElse(null);
  }
}
