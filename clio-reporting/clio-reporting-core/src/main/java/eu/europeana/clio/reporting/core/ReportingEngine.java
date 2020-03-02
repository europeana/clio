package eu.europeana.clio.reporting.core;

import eu.europeana.clio.common.persistence.StreamResult;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneId;
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
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This class provides core functionality for the reporting module of Clio.
 */
public final class ReportingEngine {

  private final AbstractPropertiesHolder properties;

  private static final DateTimeFormatter fileNameFormatter = DateTimeFormatter
          .ofPattern("'clio_report_'yyyy-MM-dd_kk-mm-ss'.csv'").withZone(ZoneId.systemDefault());

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

    // Write the report.
    try (final ClioPersistenceConnection databaseConnection = properties
            .getPersistenceConnectionProvider().createPersistenceConnection();
            final StreamResult<Pair<Run, Link>> brokenLinks = new LinkDao(databaseConnection)
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

      // Create link stream ... see https://github.com/spotbugs/spotbugs/issues/756
      @SuppressWarnings("findbugs:RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
      final Stream<Pair<Run, Link>> linkStream = brokenLinks.get();

      // Write records
      linkStream.forEach(link -> writer.writeNext(new String[]{
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

  public static String getReportFileNameSuggestion() {
    return fileNameFormatter.format(Instant.now());
  }
}
