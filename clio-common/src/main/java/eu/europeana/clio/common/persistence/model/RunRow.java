package eu.europeana.clio.common.persistence.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This represents the persistent form of a run (a checking iteration for a given dataset).
 */
@Entity
@Table(name = "run",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dataset_id", "starting_time"}))
@NamedQuery(name = RunRow.GET_ACTIVE_RUN_FOR_DATASET, query = "SELECT r"
        + " FROM RunRow AS r"
        + " WHERE r.dataset.datasetId = :" + RunRow.DATASET_ID_PARAMETER
        + " AND EXISTS("
        + "   SELECT l FROM LinkRow l WHERE l.run = r AND l.checkingTime IS NULL"
        + " )")
public class RunRow {

  public static final String GET_ACTIVE_RUN_FOR_DATASET = "getActiveRunForDataset";
  public static final String DATASET_ID_PARAMETER = "datasetId";

  @Id
  @Column(name = "run_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long runId;

  @Column(name = "starting_time", nullable = false, updatable = false)
  private long startingTime;

  @ManyToOne
  @JoinColumn(name = "dataset_id", referencedColumnName = "dataset_id", nullable = false, updatable = false)
  private DatasetRow dataset;

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
   */
  public RunRow(Instant startingTime, DatasetRow dataset) {
    this.startingTime = startingTime.toEpochMilli();
    this.dataset = dataset;
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
}
