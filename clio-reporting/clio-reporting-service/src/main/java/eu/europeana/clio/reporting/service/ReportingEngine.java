package eu.europeana.clio.reporting.service;

import com.opencsv.CSVWriter;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.BatchWithCounters;
import eu.europeana.clio.common.model.DatasetSummary;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.Report;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.dao.BatchDao;
import eu.europeana.clio.common.persistence.dao.DatasetDao;
import eu.europeana.clio.common.persistence.dao.LinkDao;
import eu.europeana.clio.common.persistence.dao.LinkDao.RunWithLink;
import eu.europeana.clio.common.persistence.dao.ReportDao;
import eu.europeana.clio.common.persistence.dao.RunDao;
import eu.europeana.clio.reporting.service.config.ReportingEngineConfiguration;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides core functionality for the reporting module of Clio.
 */
@Slf4j
@RequiredArgsConstructor
public final class ReportingEngine {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd_kk-mm-ss").withZone(ZoneOffset.UTC);
    private static final String CLIO_REPORT_PREFIX = "clio_report";
    private static final String CLIO_REPORT_SUFFIX = "csv";
    private final ReportingEngineConfiguration reportingEngineConfiguration;

    /**
     * Store the report file.
     *
     * @param report the report file in String format
     * @throws ClioException if an error occurred during the storing of the report
     */
    public void storeReport(String report) throws ClioException {
        final BatchDao batchDao = new BatchDao(reportingEngineConfiguration.sessionFactory());
        final ReportDao reportDao = new ReportDao(reportingEngineConfiguration.sessionFactory());

        BatchWithCounters latestBatch = batchDao.getLatestBatches(1).stream().findFirst().orElse(null);
        if (latestBatch != null) {
            reportDao.saveReport(report, latestBatch.getBatchId());
        }

    }

    /**
     * Generate an im memory {@link String} report.
     *
     * @return the report
     * @throws ClioException if an error occurred during generating the report
     */
    public String generateReport() throws ClioException {
        StringWriter stringWriter = new StringWriter();
        generateReport(stringWriter, null);
        return stringWriter.toString();
    }

    /**
     *  Generates a report and saves it to the output file.
     *
     *  @param writer The destination/output writer.
     * @param filters the filters
     * @throws ClioException In case of a problem with accessing or saving the required data.
     */
    public void generateReport(Writer writer, FieldFilters filters) throws ClioException {

        final long startTime = System.nanoTime();
        // Write the report. We use a try-with-resources block to ensure that all resources are properly closed after use.
        try (final StreamResult<RunWithLink> brokenLinks = getLinkDaoStreamResult(filters);
            final CSVWriter csvWriter = new CSVWriter(writer)) {
            // Write header
            csvWriter.writeNext(new String[]{
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

            // Create link stream ...
           final Stream<RunWithLink> linkStream = brokenLinks.get();

            // Write records
            linkStream.forEach(item -> csvWriter.writeNext(new String[]{
                sanitizeCsvField(item.run().getDataset().getDatasetId()),
                sanitizeCsvField(String.format(reportingEngineConfiguration
                        .clioConfigurationProperties().datasetReportLinkTemplate(),
                    item.run().getDataset().getDatasetId())),
                sanitizeCsvField(Optional.ofNullable(item.run().getDataset().getSize())
                                         .map(Object::toString).orElse(null)),
                sanitizeCsvField(item.run().getDataset().getProvider()),
                sanitizeCsvField(item.run().getDataset().getDataProvider()),
                sanitizeCsvField(item.link().getRecordId()),
                sanitizeCsvField(convert(item.link().getRecordLastIndexTime())),
                sanitizeCsvField(item.link().getRecordEdmType()),
                sanitizeCsvField(item.link().getRecordContentTier()),
                sanitizeCsvField(item.link().getRecordMetadataTier()),
                sanitizeCsvField(item.link().getLinkType().getHumanReadableName()),
                sanitizeCsvField(item.link().getLinkUrl()),
                sanitizeCsvField(item.link().getServer()),
                sanitizeCsvField(convert(item.link().getCheckingTime())),
                sanitizeCsvField(item.link().getError())
            }));
        } catch (IOException e) {
            throw new ClioException("Error occurred while compiling the report.", e);
        }

        final long elapsedTimeInSeconds = Duration.of(System.nanoTime() - startTime, ChronoUnit.NANOS).toSeconds();
        log.info("Total time elapsed in seconds: {}", elapsedTimeInSeconds);
    }

    private StreamResult<RunWithLink> getLinkDaoStreamResult(FieldFilters filters) throws PersistenceException {
        final LinkDao linkDao = new LinkDao(reportingEngineConfiguration.sessionFactory());
        return filters == null ? linkDao.getBrokenLinksInLatestCompletedRuns() : linkDao.getLinksWithRunsForFilters(filters);
    }

    private static String convert(Instant instant) {
        return instant == null ? null
                : DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * Gets report file name suggestion.
     *
     * @return the report file name suggestion
     */
    public static String getReportFileNameSuggestion() {
        return String.format("%s_%s.%s", CLIO_REPORT_PREFIX, DATE_TIME_FORMATTER.format(Instant.now()), CLIO_REPORT_SUFFIX);
    }

    /**
     * Get a file name suggestion.
     *
     * @param report the report
     * @return the file name suggestion
     */
    public static String getReportFileNameSuggestion(Report report) {
        return String.format("%s_%s_%s.%s",
                CLIO_REPORT_PREFIX, report.getBatchId(), DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(report.getCreationTime())), CLIO_REPORT_SUFFIX);
    }

    /**
     * Compiles information on the latest executed batches.
     *
     * @param maxResults The maximum number of batches to return.
     * @return The batches, in reverse chronological order.
     * @throws PersistenceException In case there was a problem with accessing the data.
     */
    public List<BatchWithCounters> getLatestBatches(int maxResults) throws PersistenceException {
        return new BatchDao(reportingEngineConfiguration.sessionFactory()).getLatestBatches(maxResults);
    }

    /**
     * Get the latest reports.
     *
     * @param maxResults maximum results to return
     * @return the list of reports
     * @throws PersistenceException in case of a persistence exception
     */
    public List<Report> getLatestReports(int maxResults) throws PersistenceException {
        return new ReportDao(reportingEngineConfiguration.sessionFactory()).getLatestReports(maxResults);
    }

    /**
     * Get all report details.
     *
     * @return the list of report details
     * @throws PersistenceException in case of a persistence exception
     */
    public List<Report> getAllReportDetails() throws PersistenceException {
        return new ReportDao(reportingEngineConfiguration.sessionFactory()).getAllReportDetails();
    }

    /**
     * Get a report by its creation time.
     *
     * @param batchId the batch id
     * @return the report
     * @throws PersistenceException if there was an error while getting the report
     */
    public Report getReportByBatchId(Long batchId) throws PersistenceException {
        return new ReportDao(reportingEngineConfiguration.sessionFactory()).getReportByBatchId(batchId);
    }

    /**
     * Get a report by its id.
     *
     * @param reportId the report id
     * @return the report
     * @throws PersistenceException if there was an error while getting the report
     */
    public Report getReportByReportId(Long reportId) throws PersistenceException {
        return new ReportDao(reportingEngineConfiguration.sessionFactory()).getReportByReportId(reportId);
    }


    /**
     * Get a summary of datasets for the given filters by finding records processed
     * by the Clio Link Checking Service.
     *
     * @param clioFilters the clio filters
     * @return the dataset summary
     * @throws PersistenceException the persistence exception
     */
    public List<DatasetSummary> findDatasetsSummary(FieldFilters clioFilters) throws PersistenceException {
        return new DatasetDao(reportingEngineConfiguration.sessionFactory()).findDatasetsSummary(clioFilters);
    }

    /**
     * Find datasets summary filter options field filters.
     *
     * @param clioFilters the clio filters
     * @return the field filters
     * @throws PersistenceException the persistence exception
     */
    public FieldFilters findDatasetsSummaryFilterOptions(FieldFilters clioFilters) throws PersistenceException {
        return new DatasetDao(reportingEngineConfiguration.sessionFactory()).findDatasetsSummaryFilterOptions(clioFilters);
    }

    /**
     * Sanitizes a CSV field to mitigate CSV injection attacks.
     * If the value starts with any of the characters =, +, -, @ it will be prefixed with a single quote (').
     * Null values are preserved.
     */
    private static String sanitizeCsvField(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }

}
