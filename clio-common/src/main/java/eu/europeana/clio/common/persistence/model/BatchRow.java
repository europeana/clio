package eu.europeana.clio.common.persistence.model;

import javax.persistence.*;
import java.time.Instant;

/**
 * This represents the persistent form of a batch (of runs).
 */
@Entity
@Table(name = "batch")
@NamedQuery(name = BatchRow.GET_LATEST_BATCHES_QUERY,
        query = "SELECT b FROM BatchRow b ORDER BY b.creationTime DESC ")
@NamedQuery(name = BatchRow.GET_OLD_BATCHES_QUERY, query = "SELECT b FROM BatchRow b WHERE b.creationTime <= :" +
        BatchRow.CREATION_TIME_PARAMETER)
@NamedQuery(name = BatchRow.DELETE_OLD_BATCHES_QUERY, query = "DELETE FROM BatchRow b WHERE b.creationTime <= :" +
        BatchRow.CREATION_TIME_PARAMETER)
public class BatchRow {

    public static final String GET_LATEST_BATCHES_QUERY = "getLatestBatches";
    public static final String GET_OLD_BATCHES_QUERY = "getOldBatches";
    public static final String DELETE_OLD_BATCHES_QUERY = "deleteOldBatches";
    public static final String CREATION_TIME_PARAMETER = "creationTime";

    @Id
    @Column(name = "batch_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long batchId;

    @Column(name = "creation_time", nullable = false, updatable = false)
    private long creationTime;

    @Column(name = "last_update_time_solr", nullable = false, updatable = false)
    private long lastUpdateTimeInSolr;

    @Column(name = "last_update_time_metis_core", nullable = false, updatable = false)
    private long lastUpdateTimeInMetisCore;

    @Column(name = "datasets_excluded_already_running")
    private Integer datasetsExcludedAlreadyRunning;

    @Column(name = "datasets_excluded_not_indexed")
    private Integer datasetsExcludedNotIndexed;

    @Column(name = "datasets_excluded_without_links")
    private Integer datasetsExcludedWithoutLinks;

    /**
     * Constructor for the use of JPA. Don't use from code.
     */
    protected BatchRow() {
    }

    public BatchRow(Instant creationTime, Instant lastUpdateTimeInSolr,
                    Instant lastUpdateTimeInMetisCore) {
        this.creationTime = creationTime.toEpochMilli();
        this.lastUpdateTimeInSolr = lastUpdateTimeInSolr.toEpochMilli();
        this.lastUpdateTimeInMetisCore = lastUpdateTimeInMetisCore.toEpochMilli();
    }

    public void setCounters(int datasetsExcludedAlreadyRunning, int datasetsExcludedNotIndexed,
                            int datasetsExcludedWithoutLinks) {
        this.datasetsExcludedAlreadyRunning = datasetsExcludedAlreadyRunning;
        this.datasetsExcludedNotIndexed = datasetsExcludedNotIndexed;
        this.datasetsExcludedWithoutLinks = datasetsExcludedWithoutLinks;
    }

    public long getBatchId() {
        return batchId;
    }

    public Instant getCreationTime() {
        return Instant.ofEpochMilli(creationTime);
    }

    public Instant getLastUpdateTimeInSolr() {
        return Instant.ofEpochMilli(lastUpdateTimeInSolr);
    }

    public Instant getLastUpdateTimeInMetisCore() {
        return Instant.ofEpochMilli(lastUpdateTimeInMetisCore);
    }

    public Integer getDatasetsExcludedAlreadyRunning() {
        return datasetsExcludedAlreadyRunning;
    }

    public Integer getDatasetsExcludedNotIndexed() {
        return datasetsExcludedNotIndexed;
    }

    public Integer getDatasetsExcludedWithoutLinks() {
        return datasetsExcludedWithoutLinks;
    }
}
