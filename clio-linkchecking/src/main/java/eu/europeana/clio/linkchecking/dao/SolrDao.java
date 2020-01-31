package eu.europeana.clio.linkchecking.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.LinkType;
import eu.europeana.clio.linkchecking.model.SampleRecord;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

/**
 * Data access object for the Solr.
 */
public class SolrDao {

  private static final String IS_SHOWN_AT_FIELD = "provider_aggregation_edm_isShownAt";
  private static final String IS_SHOWN_BY_FIELD = "provider_aggregation_edm_isShownBy";
  private static final String RECORD_ID_FIELD = "europeana_id";

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
   * Get random sample records for a given dataset.
   *
   * @param datasetId The (Metis) dataset ID of the dataset for which to get random records.
   * @param numberOfSampleRecords The number of random records needed.
   * @return The list containing the required number of random records (or fewer if the dataset
   * doesn't have that many).
   * @throws PersistenceException In case of problems obtaining the sample records.
   */
  public List<SampleRecord> getRandomSampleRecords(String datasetId, int numberOfSampleRecords)
          throws PersistenceException {

    // Create query
    final SolrQuery solrQuery = new SolrQuery("*.*");
    solrQuery.setFilterQueries("edm_datasetName:" + datasetId + "_*");
    solrQuery.setSort(new SortClause("random_" + System.currentTimeMillis(), ORDER.asc));
    solrQuery.setStart(0);
    solrQuery.setRows(numberOfSampleRecords);
    solrQuery.setFields(IS_SHOWN_AT_FIELD, IS_SHOWN_BY_FIELD, RECORD_ID_FIELD);

    // Execute query
    final QueryResponse queryResult;
    try {
      queryResult = this.solrClient.query(solrQuery);
    } catch (SolrServerException | IOException e) {
      throw new PersistenceException("Problem occurred while obtaining a sample record.", e);
    }

    // Get result.
    return queryResult.getResults().stream().map(result -> {
      final Map<LinkType, Set<String>> links = new EnumMap<>(LinkType.class);
      final List<?> isShownAtLinks = (List<?>) result.getFieldValue(IS_SHOWN_AT_FIELD);
      if (isShownAtLinks != null) {
        for (Object link : isShownAtLinks) {
          links.computeIfAbsent(LinkType.IS_SHOWN_AT, key -> new HashSet<>()).add(link.toString());
        }
      }
      final List<?> isShownByLinks = (List<?>) result.getFieldValue(IS_SHOWN_BY_FIELD);
      if (isShownByLinks != null) {
        for (Object link : isShownByLinks) {
          links.computeIfAbsent(LinkType.IS_SHOWN_BY, key -> new HashSet<>()).add(link.toString());
        }
      }
      return new SampleRecord((String) result.getFieldValue(RECORD_ID_FIELD), links);
    }).collect(Collectors.toList());
  }
}
