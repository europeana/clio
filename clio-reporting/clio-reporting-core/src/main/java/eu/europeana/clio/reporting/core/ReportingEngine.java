package eu.europeana.clio.reporting.core;

import eu.europeana.clio.common.persistence.StreamResult;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import com.opencsv.CSVWriter;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.dao.LinkDao;
import eu.europeana.clio.reporting.core.config.AbstractPropertiesHolder;
import org.apache.commons.lang3.tuple.Pair;

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
    final ClioPersistenceConnection databaseConnection =
        properties.getPersistenceConnectionProvider().createPersistenceConnection();

    // Write the report.
    try (final StreamResult<Pair<Run, Link>> brokenLinks = new LinkDao(databaseConnection)
            .getBrokenLinksInLatestCompletedRuns();
            final CSVWriter writer = new CSVWriter(output)) {

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
      brokenLinks.get().forEach(link -> writer.writeNext(new String[]{
              link.getLeft().getDataset().getDatasetId(),
              Optional.ofNullable(link.getLeft().getDataset().getSize())
                      .map(Object::toString).orElse(null),
              convert(link.getLeft().getDataset().getLastIndexTime()),
              link.getRight().getLinkType().getHumanReadableName(),
              link.getRight().getLinkUrl(),
              link.getRight().getServer(),
              convert(link.getRight().getCheckingTime()),
              link.getRight().getError()
      }));
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
