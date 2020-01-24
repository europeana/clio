package eu.europeana.clio.reporting.core;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import com.opencsv.CSVWriter;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.dao.LinkDao;
import eu.europeana.clio.reporting.core.config.AbstractPropertiesHolder;

/**
 * This class provides core functionality for the reporting module of Clio.
 */
public final class ReportingEngine {

  private final AbstractPropertiesHolder properties;

  /**
   * Constrcutor.
   *
   * @param properties The properties of this module.
   */
  public ReportingEngine(AbstractPropertiesHolder properties) {
    this.properties = properties;
  }

  /**
   * Generates a report and saves it to the output file.
   *
   * @param output The destination/output writer.
   * @throws ClioException In case of a problem with accessing or saving the required data.
   */
  public void generateReport(Writer output) throws ClioException {

    // TODO in the furute we can cache the result and invalidate/recompute it as soon as we have a
    // new execution (we can check the most recent run starting time).

    // Get the broken links.
    // TODO this may be thousands of links (or more). We should not get this all at once.
    final ClioPersistenceConnection databaseConnection =
        properties.getPersistenceConnectionProvider().createPersistenceConnection();
    final SortedMap<Run, List<Link>> brokenLinks = new LinkDao(databaseConnection)
            .getBrokenLinksInLatestCompletedRuns();

    // Write the report.
    try (final CSVWriter writer = new CSVWriter(output)) {

      // Write header
      writer.writeNext(new String[]{
              "Dataset ID",
              "Dataset size",
              "Last index",
              "Link type",
              "Link",
              "Link server",
              "Time of checking",
              "Error"
      });

      // Write records
      brokenLinks.forEach((run, links) -> links.forEach(link ->
                      writer.writeNext(new String[]{
                              run.getDataset().getDatasetId(),
                              Optional.ofNullable(run.getDataset().getSize()).map(Object::toString).orElse(
                                      null),
                              convert(run.getDataset().getLastIndexTime()),
                              link.getLinkType().getHumanReadableName(),
                              link.getLinkUrl(),
                              link.getServer(),
                              convert(link.getCheckingTime()),
                              link.getError()
                      })
              )
      );
    } catch (IOException e) {
      throw new ClioException("Error occurred while compiling the report.", e);
    }
  }

  private static String convert(Instant instant) {
    return instant == null ? null
            : DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.systemDefault())
                    .format(instant);
  }
}
