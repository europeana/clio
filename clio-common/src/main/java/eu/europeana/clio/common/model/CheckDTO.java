package eu.europeana.clio.common.model;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;


/**
 * The type Check dto.
 */
@Setter
@Getter
public class CheckDTO {

  private long runId;

  private Dataset dataset;

  private long errorLinks;

  private long totalLinks;

  private Instant startingTime;

  private int percentLinksInOperation;

  /**
   * Instantiates a new Check dto.
   *
   * @param runId the run id
   * @param dataset the dataset
   * @param startingTime the starting time
   * @param errorLinks the error links
   * @param totalLinks the total links
   */
  public CheckDTO(long runId, Dataset dataset, Instant startingTime, long errorLinks, long totalLinks) {
    this.runId = runId;
    this.dataset = dataset;
    this.startingTime = startingTime;
    this.percentLinksInOperation = (int) (errorLinks / (double) totalLinks * 100);
  }

}
