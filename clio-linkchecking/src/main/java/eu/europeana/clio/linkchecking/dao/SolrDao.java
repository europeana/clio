package eu.europeana.clio.linkchecking.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.LinkType;
import eu.europeana.clio.linkchecking.model.SampleRecord;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data access object for the Solr.
 */
public class SolrDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrDao.class);

    private static final String CONTENT_TIER_FIELD = "contentTier";
    private static final String EDM_DATASET_NAME_FIELD = "edm_datasetName";
    private static final String EDM_TYPE_FIELD = "proxy_edm_type";
    private static final String IS_SHOWN_AT_FIELD = "provider_aggregation_edm_isShownAt";
    private static final String IS_SHOWN_BY_FIELD = "provider_aggregation_edm_isShownBy";
    private static final String METADATA_TIER_FIELD = "metadataTier";
    private static final String RECORD_ID_FIELD = "europeana_id";
    private static final String TIMESTAMP_UPDATE_FIELD = "timestamp_update";

    private final SolrClient solrClient;

    /**
     * Constructor.
     *
     * @param solrClient The Solr client.
     */
    public SolrDao(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    /**
     * Get random sample records for a given dataset that have at least one link.
     *
     * @param datasetId             The (Metis) dataset ID of the dataset for which to get random records.
     * @param numberOfSampleRecords The number of random records needed.
     * @return The list containing the required number of random records (or fewer if the dataset
     * doesn't have that many).
     * @throws PersistenceException In case of problems obtaining the sample records.
     */
    public List<SampleRecord> getRandomSampleRecords(String datasetId, int numberOfSampleRecords)
            throws PersistenceException {

        // Create query
        final SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.setFilterQueries(EDM_DATASET_NAME_FIELD + ":" + datasetId + "_*",
                IS_SHOWN_AT_FIELD + ":[* TO *] OR " + IS_SHOWN_BY_FIELD + ":[* TO *]");
        solrQuery.setSort(new SortClause("random_" + System.currentTimeMillis(), ORDER.asc));
        solrQuery.setStart(0);
        solrQuery.setRows(numberOfSampleRecords);
        solrQuery.setFields(CONTENT_TIER_FIELD, EDM_TYPE_FIELD, IS_SHOWN_AT_FIELD, IS_SHOWN_BY_FIELD,
                METADATA_TIER_FIELD, RECORD_ID_FIELD, TIMESTAMP_UPDATE_FIELD);

        // Get and return result.
        return executeQuery(solrQuery).stream().map(SolrDao::convert).collect(Collectors.toList());
    }

    private static SampleRecord convert(SolrDocument result) {

        // Get the isShownAt links.
        final Map<LinkType, Set<String>> links = new EnumMap<>(LinkType.class);
        final List<?> isShownAtLinks = (List<?>) result.getFieldValue(IS_SHOWN_AT_FIELD);
        if (isShownAtLinks != null) {
            for (Object link : isShownAtLinks) {
                links.computeIfAbsent(LinkType.IS_SHOWN_AT, key -> new HashSet<>()).add(link.toString());
            }
        }

        // Get the isShownBy links.
        final List<?> isShownByLinks = (List<?>) result.getFieldValue(IS_SHOWN_BY_FIELD);
        if (isShownByLinks != null) {
            for (Object link : isShownByLinks) {
                links.computeIfAbsent(LinkType.IS_SHOWN_BY, key -> new HashSet<>()).add(link.toString());
            }
        }

        // Get the last indexed time (update time). This should really exist.
        final Instant lastIndexedTime = Optional
                .ofNullable(result.getFieldValue(TIMESTAMP_UPDATE_FIELD)).map(Date.class::cast)
                .map(Date::toInstant).orElse(Instant.EPOCH);

        // Get the edm:type.
        final List<String> edmTypes = Optional
                .ofNullable((List<?>) result.getFieldValue(EDM_TYPE_FIELD)).stream()
                .flatMap(Collection::stream).filter(Objects::nonNull).map(String.class::cast)
                .filter(type -> !type.isBlank()).distinct().collect(Collectors.toList());
        final String recordId = (String) result.getFieldValue(RECORD_ID_FIELD);
        if (edmTypes.size() > 1 && LOGGER.isInfoEnabled()) {
            LOGGER.info("Found multiple types for record '{}': {}", recordId,
                    String.join(", ", edmTypes));
        }
        final String edmType = edmTypes.isEmpty() ? null : edmTypes.get(0);

        // Done.
        return new SampleRecord(recordId, lastIndexedTime, edmType,
                (String) result.getFieldValue(CONTENT_TIER_FIELD),
                (String) result.getFieldValue(METADATA_TIER_FIELD), links);
    }

    /**
     * Determines the last time that there was an update (i.e. a record was indexed). We do this by
     * returning the maximum of all update timestamps.
     *
     * @return The last update instant.
     */
    public Instant getLastUpdateTime() throws PersistenceException {

        // Create query
        final SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.setSort(new SortClause(TIMESTAMP_UPDATE_FIELD, ORDER.desc));
        solrQuery.setStart(0);
        solrQuery.setRows(1);
        solrQuery.setFields(TIMESTAMP_UPDATE_FIELD);

        // Get and return result
        return executeQuery(solrQuery).stream().findFirst()
                .map(document -> document.getFieldValue(TIMESTAMP_UPDATE_FIELD)).map(Date.class::cast)
                .map(Date::toInstant).orElse(Instant.EPOCH);
    }

    private SolrDocumentList executeQuery(SolrQuery solrQuery) throws PersistenceException {
        try {
            return this.solrClient.query(solrQuery).getResults();
        } catch (SolrServerException | IOException e) {
            throw new PersistenceException("Problem occurred while obtaining the last update time.", e);
        }
    }
}
