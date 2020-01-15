package eu.europeana.clio.common.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "run")
public class RunRow {

  @Id
  @Column(name = "run_id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long runId;

  @Column(name = "starting_time", nullable = false, updatable = false)
  private long startingTime;

  @ManyToOne
  @JoinColumn(name = "dataset_id", referencedColumnName = "dataset_id", updatable = false, nullable = false)
  private DatasetRow dataset;

  public RunRow() {
  }

  public RunRow(long startingTime, DatasetRow dataset) {
    this.startingTime = startingTime;
    this.dataset = dataset;
  }

  public long getRunId() {
    return runId;
  }

  public long getStartingTime() {
    return startingTime;
  }

  public DatasetRow getDataset() {
    return dataset;
  }
}
