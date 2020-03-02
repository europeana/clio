package eu.europeana.clio.common.model;

import java.time.Instant;

/**
 * This class represents a run (a checking iteration for a given dataset).
 */
public class Run {

  private final long runId;
  private final Instant startingTime;
  private final Dataset dataset;

  /**
   * Constructor.
   *
   * @param runId The ID of the run.
   * @param startingTime The starting time of the run.
   * @param dataset The dataset to which this run belongs.
   */
  public Run(long runId, Instant startingTime, Dataset dataset) {
    this.runId = runId;
    this.startingTime = startingTime;
    this.dataset = dataset;
  }

  public long getRunId() {
    return runId;
  }

  public Instant getStartingTime() {
    return startingTime;
  }

  public Dataset getDataset() {
    return dataset;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    final Run that = (Run) obj;
    return this.runId == that.runId;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(runId);
  }
}
