package eu.europeana.clio.common.persistence.model;

import static org.apache.commons.lang3.StringUtils.truncate;

import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * This represents the persistent form of a link (to be checked once as part of a run).
 */
@Entity
@Table(name = "link", indexes = {
    @Index(name = "link_server_idx", columnList = "server"),
    @Index(name = "link_checking_time_idx", columnList = "checking_time"),
    @Index(name = "link_record_id_idx", columnList = "record_id"),
    @Index(name = "link_link_type_idx", columnList = "link_type"),
    @Index(name = "link_link_url_idx", columnList = "link_url"),
    @Index(name = "Link_run_id_idx", columnList = "run_id")})
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
  private static final int MAX_RECORD_EDM_TYPE_LENGTH = 5;
  private static final int MAX_RECORD_CONTENT_TIER_LENGTH = 1;
  private static final int MAX_RECORD_METADATA_TIER_LENGTH = 1;
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
  @JoinColumn(name = "run_id", referencedColumnName = "run_id",
      foreignKey = @ForeignKey(name = "link_run_id_fkey"), nullable = false, updatable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private RunRow run;

  @Column(name = "record_id", nullable = false, updatable = false, length = MAX_RECORD_ID_LENGTH)
  private String recordId;

  @Column(name = "record_last_index_time", nullable = false, updatable = false)
  private long recordLastIndexTime;

  @Column(name = "record_edm_type", updatable = false, length = MAX_RECORD_EDM_TYPE_LENGTH)
  private String recordEdmType;

  @Column(name = "record_content_tier", updatable = false, length = MAX_RECORD_CONTENT_TIER_LENGTH)
  private String recordContentTier;

  @Column(name = "record_metadata_tier", updatable = false, length = MAX_RECORD_METADATA_TIER_LENGTH)
  private String recordMetadataTier;

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
   * Constructor. Creates a link in an unchecked state. If the string of either the record ID, the link URL or the server is too
   * long to fit in the field, this instance will be in a state of being checked with errors.
   *
   * @param run The run to which this link belongs.
   * @param recordId The Europeana record ID in which this link is present.
   * @param recordLastIndexTime The last time this record was indexed.
   * @param recordEdmType The edm:type of the record.
   * @param recordContentTier The content tier of the record.
   * @param recordMetadataTier The metadata tier of the record.
   * @param linkType The type of the link reference in the record.
   * @param linkUrl The actual link.
   * @param server The server of the link. Can be null if the server could not be computed.
   */
  public LinkRow(RunRow run, String recordId, Instant recordLastIndexTime, String recordEdmType,
      String recordContentTier, String recordMetadataTier, LinkType linkType, String linkUrl,
      String server) {

    // Set basic properties
    this.run = run;
    this.recordId = truncate(recordId, MAX_RECORD_ID_LENGTH);
    this.recordLastIndexTime = recordLastIndexTime.toEpochMilli();
    this.recordEdmType = recordEdmType;
    this.recordContentTier = recordContentTier;
    this.recordMetadataTier = recordMetadataTier;
    this.linkType = linkType;
    this.linkUrl = truncate(linkUrl, MAX_LINK_URL_LENGTH);
    this.server = truncate(server, MAX_SERVER_LENGTH);

    // Test the length of certain properties
    final String errorString;
    if (recordId.length() > MAX_RECORD_ID_LENGTH) {
      errorString = "Record ID is too long: " + recordId;
    } else if (recordEdmType != null && recordEdmType.length() > MAX_RECORD_EDM_TYPE_LENGTH) {
      errorString = "Record edm:type is too long: " + recordEdmType;
    } else if (recordContentTier != null
        && recordContentTier.length() > MAX_RECORD_CONTENT_TIER_LENGTH) {
      errorString = "Record content tier is too long: " + recordContentTier;
    } else if (recordMetadataTier != null
        && recordMetadataTier.length() > MAX_RECORD_METADATA_TIER_LENGTH) {
      errorString = "Record metadata tier is too long: " + recordMetadataTier;
    } else if (linkUrl.length() > MAX_LINK_URL_LENGTH) {
      errorString = "Link URL is too long: " + linkUrl;
    } else if (server == null) {
      errorString = "Server could not be determined for link: " + linkUrl;
    } else if (server.length() > MAX_SERVER_LENGTH) {
      errorString = "Server is too long: " + server;
    } else {
      errorString = null;
    }
    if (errorString != null) {
      this.error = truncate(errorString, MAX_ERROR_LENGTH);
      this.checkingTime = Instant.now().toEpochMilli();
    }
  }

  public void setError(String error) {
    this.error = truncate(error, MAX_ERROR_LENGTH);
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
    return Optional.ofNullable(checkingTime).map(Instant::ofEpochMilli).orElse(null);
  }
}
