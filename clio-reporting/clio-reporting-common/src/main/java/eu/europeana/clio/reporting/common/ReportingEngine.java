package eu.europeana.clio.reporting.common;

import com.opencsv.CSVWriter;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.BatchWithCounters;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.dao.BatchDao;
import eu.europeana.clio.common.persistence.dao.LinkDao;
import eu.europeana.clio.reporting.common.config.ReportingEngineConfiguration;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * This class provides core functionality for the reporting module of Clio.
 */
public final class ReportingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ReportingEngineConfiguration properties;

    private static final DateTimeFormatter fileNameFormatter = DateTimeFormatter
            .ofPattern("'clio_report_'yyyy-MM-dd_kk-mm-ss'.csv'").withZone(ZoneId.systemDefault());

    /**
     * Constrcutor.
     *
     * @param properties The properties of this module.
     */
    public ReportingEngine(ReportingEngineConfiguration properties) {
        this.properties = properties;
    }

    /**
     * Generates a report and saves it to the output file.
     *
     * @param output The destination/output writer.
     * @throws ClioException In case of a problem with accessing or saving the required data.
     */
    public void generateReport(Writer output) throws ClioException {

        final long startTime = System.nanoTime();
        // Write the report.
        try (final ClioPersistenceConnection databaseConnection = properties
                .getPersistenceConnectionProvider().createPersistenceConnection();
             final StreamResult<Pair<Run, Link>> brokenLinks = new LinkDao(databaseConnection)
                     .getBrokenLinksInLatestCompletedRuns();
             final CSVWriter writer = new CSVWriter(output)) {

            // Write header
            writer.writeNext(new String[]{
                    "Dataset ID",
                    "Dataset's Metis page",
                    "Dataset size",
                    "Provider",
                    "Data provider",
                    "Record ID",
                    "Last record index",
                    "Record edm:type",
                    "Record content tier",
                    "Record metadata tier",
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
                    String.format(properties.getReportDatasetLinkTemplate(),
                            link.getLeft().getDataset().getDatasetId()),
                    Optional.ofNullable(link.getLeft().getDataset().getSize())
                            .map(Object::toString).orElse(null),
                    link.getLeft().getDataset().getProvider(),
                    link.getLeft().getDataset().getDataProvider(),
                    link.getRight().getRecordId(),
                    convert(link.getRight().getRecordLastIndexTime()),
                    link.getRight().getRecordEdmType(),
                    link.getRight().getRecordContentTier(),
                    link.getRight().getRecordMetadataTier(),
                    link.getRight().getLinkType().getHumanReadableName(),
                    link.getRight().getLinkUrl(),
                    link.getRight().getServer(),
                    convert(link.getRight().getCheckingTime()),
                    link.getRight().getError()
            }));
        } catch (IOException e) {
            throw new ClioException("Error occurred while compiling the report.", e);
        }

        final long elapsedTimeInSeconds = Duration.of(System.nanoTime() - startTime, ChronoUnit.NANOS).toSeconds();
        LOGGER.info("Total time elapsed in seconds: {}", elapsedTimeInSeconds);
    }

    private static String convert(Instant instant) {
        return instant == null ? null
                : DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
                .format(instant);
    }

    public static String getReportFileNameSuggestion() {
        return fileNameFormatter.format(Instant.now());
    }

    /**
     * Compiles information on the latest executed batches.
     *
     * @param maxResults The maximum number of batches to return.
     * @return The batches, in reverse chronological order.
     * @throws PersistenceException In case there was a problem with accessing the data.
     */
    public List<BatchWithCounters> getLatestBatches(int maxResults) throws PersistenceException {
        try (final ClioPersistenceConnection databaseConnection = properties
                .getPersistenceConnectionProvider().createPersistenceConnection()) {
            return new BatchDao(databaseConnection).getLatestBatches(maxResults);
        }
    }
}
