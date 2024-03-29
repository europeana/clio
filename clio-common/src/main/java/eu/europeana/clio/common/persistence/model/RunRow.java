package eu.europeana.clio.common.persistence.model;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * This represents the persistent form of a run (a checking iteration for a given dataset).
 */
@Entity
@Table(name = "run", indexes = {
    @Index(name = "run_starting_time_idx", columnList = "starting_time"),
    @Index(name = "run_batch_id_idx", columnList = "batch_id"),
    @Index(name = "run_dataset_id_idx", columnList = "dataset_id")})
@NamedQuery(name = RunRow.GET_ACTIVE_RUN_FOR_DATASET, query = "SELECT r"
    + " FROM RunRow AS r"
    + " WHERE r.dataset.datasetId = :" + RunRow.DATASET_ID_PARAMETER
    + " AND EXISTS("
    + "   SELECT l FROM LinkRow l WHERE l.run = r AND l.checkingTime IS NULL"
    + " )")
@NamedQuery(name = RunRow.COUNT_RUNS_FOR_BATCH, query = "SELECT COUNT(r) FROM RunRow AS r"
    + " WHERE r.batch.batchId = :" + RunRow.BATCH_ID_PARAMETER)
@NamedQuery(name = RunRow.COUNT_PENDING_RUNS_FOR_BATCH, query = "SELECT COUNT(r) FROM RunRow AS r"
    + " WHERE r.batch.batchId = :" + RunRow.BATCH_ID_PARAMETER + " AND EXISTS("
    + "   SELECT l FROM LinkRow l WHERE l.run = r AND l.checkingTime IS NULL"
    + " )")
public class RunRow {

  public static final String COUNT_RUNS_FOR_BATCH = "countRunsForBatch";
  public static final String COUNT_PENDING_RUNS_FOR_BATCH = "countPendingRunsForBatch";
  public static final String GET_ACTIVE_RUN_FOR_DATASET = "getActiveRunForDataset";
  public static final String DATASET_ID_PARAMETER = "datasetId";
  public static final String BATCH_ID_PARAMETER = "batchId";

  @Id
  @Column(name = "run_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long runId;

  @Column(name = "starting_time", nullable = false, updatable = false)
  private long startingTime;

  @ManyToOne()
  @JoinColumn(name = "dataset_id", referencedColumnName = "dataset_id",
      foreignKey = @ForeignKey(name = "run_dataset_id_fkey"), nullable = false, updatable = false)
  private DatasetRow dataset;

  @ManyToOne
  @JoinColumn(name = "batch_id", referencedColumnName = "batch_id",
      foreignKey = @ForeignKey(name = "run_batch_id_fkey"), nullable = false, updatable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private BatchRow batch;

  /**
   * Constructor for the use of JPA. Don't use from code.
   */
  protected RunRow() {
  }

  /**
   * Constructor.
   *
   * @param startingTime The starting time of this run.
   * @param dataset The dataset to which this run belongs.
   * @param batch The batch to which this run belongs.
   */
  public RunRow(Instant startingTime, DatasetRow dataset, BatchRow batch) {
    this.startingTime = startingTime.toEpochMilli();
    this.dataset = dataset;
    this.batch = batch;
  }

  public long getRunId() {
    return runId;
  }

  public Instant getStartingTime() {
    return Instant.ofEpochMilli(startingTime);
  }

  public DatasetRow getDataset() {
    return dataset;
  }

  public BatchRow getBatch() {
    return batch;
  }
}
