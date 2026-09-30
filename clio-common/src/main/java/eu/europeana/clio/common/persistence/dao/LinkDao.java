package eu.europeana.clio.common.persistence.dao;


import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.HUNDRED;
import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.addPredicateAndParameter;
import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.addPredicateAndParameterDateRange;
import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.addPredicateAndParameterExcludedIds;
import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.addPredicateAndParameterLastNinetyDays;
import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.addPredicatePercentLinksInOperation;
import static eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.buildCommonBase;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.dao.DatasetDaoSupport.CommonDatasetQueryParts;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.LinkRow.LinkType;
import eu.europeana.clio.common.persistence.model.RunRow;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;

/**
 * Data access object for links (to be checked once as part of a run).
 */
@Slf4j
public class LinkDao {

  public static final int FETCH_SIZE = 256; // limit the amount of records per request to avoid OOM (Out of Memory)
  private final HibernateSessionUtils hibernateSessionUtils;

  /**
   * Constructor.
   *
   * @param sessionFactory The connection to the Clio persistence. Should be connected. This object does not close the
   * connection.
   */
  public LinkDao(SessionFactory sessionFactory) {
    this.hibernateSessionUtils = new HibernateSessionUtils(sessionFactory);
  }

  private static String computeServer(String url) {
    try {
      final URI uri = URI.create(url);
      if (uri.getScheme() == null || uri.getAuthority() == null) {
        return null;
      }
      return uri.getScheme() + "://" + uri.getAuthority() + "/";
    } catch (IllegalArgumentException e) {
      log.error("Error while computing server for URL {}: {}", url, e.getMessage());
      return null;
    }
  }

  private static Link convert(LinkRow row) {

    // Compute the link type.
    final eu.europeana.clio.common.model.LinkType publicLinkType = switch (row.getLinkType()) {
      case IS_SHOWN_AT -> eu.europeana.clio.common.model.LinkType.IS_SHOWN_AT;
      case IS_SHOWN_BY -> eu.europeana.clio.common.model.LinkType.IS_SHOWN_BY;
    };

    // Return link
    return new Link(row.getLinkId(), row.getRecordId(), row.getRecordLastIndexTime(),
        row.getRecordEdmType(), row.getRecordContentTier(), row.getRecordMetadataTier(),
        publicLinkType, row.getLinkUrl(), row.getServer(), row.getError(),
        row.getCheckingTime());
  }

  /**
   * Create (i.e. persist) a link that is not yet checked by Clio.
   *
   * @param uncheckedLinkData the data of the link to create. This includes the run to which this link belongs, the record in
   * which this link is present, and the link URL and type.
   * @return The ID of the link.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public long createUncheckedLink(UncheckedLinkData uncheckedLinkData) throws PersistenceException {

    // Compute the link type.
    final LinkType persistentLinkType = switch (uncheckedLinkData.linkType()) {
      case IS_SHOWN_AT -> LinkType.IS_SHOWN_AT;
      case IS_SHOWN_BY -> LinkType.IS_SHOWN_BY;
      default -> throw new IllegalStateException();
    };

    // Create and save the link
    return hibernateSessionUtils.performInTransaction(session -> {
      final RunRow runRow = session.find(RunRow.class, uncheckedLinkData.runId());
      if (runRow == null) {
        throw new PersistenceException(
            "Cannot create link: run with ID " + uncheckedLinkData.runId() + " does not exist.");
      }
      final LinkRow newLink = new LinkRow(runRow, uncheckedLinkData.recordId(), uncheckedLinkData.recordLastIndexTime(),
          uncheckedLinkData.recordEdmType(), uncheckedLinkData.recordContentTier(), uncheckedLinkData.recordMetadataTier(),
          persistentLinkType, uncheckedLinkData.linkUrl(), computeServer(uncheckedLinkData.linkUrl()));

      session.persist(newLink);
      session.flush();
      return newLink.getLinkId();
    });
  }

  /**
   * Get a stream of all links that are currently unchecked and need to be checked.
   *
   * @return An unchecked link.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public StreamResult<Link> getAllUncheckedLinks() throws PersistenceException {
    return hibernateSessionUtils.performForStream(
        session -> session.createNamedQuery(LinkRow.GET_UNCHECKED_LINKS, LinkRow.class)
                          .getResultStream().map(LinkDao::convert));
  }

  /**
   * Update a link to add the result of the link checking for this link. This method updates all unchecked links that have the
   * same link URL (there may theoretically be multiple even though it is not very likely).
   *
   * @param linkUrl The URL of the link that was checked.
   * @param error The link checking error, if any. Null otherwise.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public void registerLinkChecking(String linkUrl, String error) throws PersistenceException {
    final Instant checkingTime = Instant.now();
    hibernateSessionUtils.performInTransaction(session -> {
      final List<LinkRow> linksToUpdate = session
          .createNamedQuery(LinkRow.GET_UNCHECKED_LINKS_BY_URL, LinkRow.class)
          .setParameter(LinkRow.LINK_URL_PARAMETER, linkUrl).getResultList();
      for (LinkRow link : linksToUpdate) {
        link.setCheckingTime(checkingTime);
        link.setError(error);
      }
      return null;
    });
  }

  /**
   * This method returns all broken links that are part of a run that is the latest executed and completed run for it's dataset.
   * Essentially, this returns the current error state: for each dataset it looks at the latest completed run and returns any
   * links that are broken.
   *
   * @return A list of pairs with runs and links. The list is sorted by (Metis) dataset ID, then record ID, then link type, then
   * link URL.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public StreamResult<RunWithLink> getBrokenLinksInLatestCompletedRuns()
      throws PersistenceException {
    return hibernateSessionUtils.performForStream(session -> session
        .createNamedQuery(LinkRow.GET_BROKEN_LINKS_IN_LATEST_COMPLETED_RUNS, LinkRow.class)
        .getResultStream()
        .map(link -> new RunWithLink(RunDao.convert(link.getRun()), convert(link)))
    );
  }

  /**
   * Gets links with runs for filters.
   *
   * @param filters the filters
   * @return the links with runs for filters
   * @throws PersistenceException the persistence exception
   */
  public StreamResult<RunWithLink> getLinksWithRunsForFilters(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performForStream( session -> {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CommonDatasetQueryParts<RunWithLink> queryParts = buildCommonBase(criteriaBuilder, RunWithLink.class);

      // Subquery 1
      Subquery<String> datasetSummarySubquery = queryParts.criteriaQuery().subquery(String.class);
      Root<LinkRow> sublink = datasetSummarySubquery.from(LinkRow.class);
      Join<LinkRow, RunRow> runSq1 = sublink.join("run", JoinType.INNER);
      Join<RunRow, DatasetRow> datasetSq1 = runSq1.join("dataset", JoinType.INNER);

      List<Predicate> wherePredicatesSq1 = new ArrayList<>();
      List<Predicate> havingPredicatesSq1 = new ArrayList<>();
      Map<ParameterExpression<?>, Object> parametersMapSq1 = new HashMap<>();

      // Compute aggregations
      Expression<Long> errorsLinksSq1 = criteriaBuilder.coalesce(criteriaBuilder.count(sublink.get(FieldNames.ERROR_MESSAGE_DB)), 0).as(Long.class);
      Expression<Long> totalLinksSq1 = criteriaBuilder.coalesce(criteriaBuilder.count(sublink), 0).as(Long.class);

      Expression<Integer> percentLinksInOperationSq1 = criteriaBuilder.diff(HUNDRED,
          criteriaBuilder.prod(criteriaBuilder.<Double>selectCase()
                                              .when(criteriaBuilder.equal(totalLinksSq1, 0D), 0D)
                                              .otherwise(criteriaBuilder.quot(
                                                                            criteriaBuilder.toDouble(errorsLinksSq1), criteriaBuilder.toDouble(totalLinksSq1))
                                                                        .as(Double.class)), HUNDRED)).cast(Integer.class);

      // Apply filters
      addPredicateAndParameter(filters.getProvider(), criteriaBuilder,
          wherePredicatesSq1, datasetSq1, parametersMapSq1, FieldNames.DATASET_PROVIDER_DB);
      addPredicateAndParameter(filters.getDataProvider(), criteriaBuilder,
          wherePredicatesSq1, datasetSq1, parametersMapSq1, FieldNames.DATASET_DATA_PROVIDER_DB);
      addPredicateAndParameter(filters.getDatasetId(), criteriaBuilder,
          wherePredicatesSq1, datasetSq1, parametersMapSq1, FieldNames.DATASET_ID_DB);
      addPredicateAndParameter(filters.getDatasetName(), criteriaBuilder,
          wherePredicatesSq1, datasetSq1, parametersMapSq1, FieldNames.DATASET_NAME_DB);
      addPredicateAndParameterExcludedIds(filters.getExcludedId(), criteriaBuilder,
          wherePredicatesSq1, datasetSq1, parametersMapSq1);
      addPredicateAndParameterLastNinetyDays(criteriaBuilder,
          wherePredicatesSq1, sublink, parametersMapSq1);
      addPredicateAndParameterDateRange(filters, criteriaBuilder,
          wherePredicatesSq1, datasetSq1, parametersMapSq1);
      addPredicatePercentLinksInOperation(filters, criteriaBuilder,
          havingPredicatesSq1, percentLinksInOperationSq1, parametersMapSq1);

      // select subquery 1
      datasetSummarySubquery.select(datasetSq1.get(FieldNames.DATASET_ID_DB));

      // where & having subquery 1
      datasetSummarySubquery.where(criteriaBuilder.and(wherePredicatesSq1));
      datasetSummarySubquery.having(havingPredicatesSq1);

      // group by
      datasetSummarySubquery.groupBy(datasetSq1.get(FieldNames.DATASET_ID_DB),
          datasetSq1.get(FieldNames.DATASET_NAME_DB), datasetSq1.get(FieldNames.DATASET_SIZE),
          datasetSq1.get(FieldNames.DATASET_LAST_INDEX), datasetSq1.get(FieldNames.DATASET_PROVIDER_DB),
          datasetSq1.get(FieldNames.DATASET_DATA_PROVIDER_DB));

      // Subquery 2
      Subquery<Long> datasetCheckSummarySubquery = queryParts.criteriaQuery().subquery(Long.class);
      Root<LinkRow> sublinkSq2 = datasetCheckSummarySubquery.from(LinkRow.class);
      Join<LinkRow, RunRow> runSq2 = sublinkSq2.join("run", JoinType.INNER);

      List<Predicate> wherePredicatesSq2 = new ArrayList<>();
      List<Predicate> havingPredicatesSq2 = new ArrayList<>();
      Map<ParameterExpression<?>, Object> parametersMapSq2 = new HashMap<>();
      // where subquery 2
      wherePredicatesSq2.add(runSq2.get(FieldNames.DATASET_TABLE_NAME_DB).get(FieldNames.DATASET_ID_DB).in(datasetSummarySubquery));
      addPredicateAndParameterLastNinetyDays(criteriaBuilder, wherePredicatesSq2, sublinkSq2, parametersMapSq2);

      // select
      datasetCheckSummarySubquery.select(runSq2.get(FieldNames.RUN_ID_DB));

      // where & having
      datasetCheckSummarySubquery.where(criteriaBuilder.and(wherePredicatesSq2));
      datasetCheckSummarySubquery.having(havingPredicatesSq2);

      // group by
      datasetCheckSummarySubquery.groupBy(runSq2.get(FieldNames.STARTING_TIME_DB), runSq2.get(FieldNames.RUN_ID_DB));

      // Main query
      queryParts.wherePredicates().add(queryParts.run().get(FieldNames.RUN_ID_DB).in(datasetCheckSummarySubquery));
      CriteriaQuery<RunWithLink> criteriaQuery = queryParts.criteriaQuery();

      // select
      criteriaQuery.select(criteriaBuilder.construct(RunWithLink.class, queryParts.run(), queryParts.link()));
      // where
      criteriaQuery.where(criteriaBuilder.and(queryParts.wherePredicates()));

      // order by (specific to LinkDao)
      criteriaQuery.orderBy(
          criteriaBuilder.asc(queryParts.dataset().get(FieldNames.DATASET_ID_DB)),
          criteriaBuilder.asc(queryParts.link().get(FieldNames.RECORD_ID_DB)),
          criteriaBuilder.asc(queryParts.link().get(FieldNames.LINK_TYPE_DB)),
          criteriaBuilder.asc(queryParts.link().get(FieldNames.LINK_URL_DB))
      );

      TypedQuery<RunWithLink> query = session.createQuery(criteriaQuery);
      queryParts.parametersMap().forEach((key, value) -> query.setParameter(key.getName(), value));
      parametersMapSq1.forEach((key, value) -> query.setParameter(key.getName(), value));
      parametersMapSq2.forEach((key, value) -> query.setParameter(key.getName(), value));
      query.setHint("org.hibernate.fetchSize", FETCH_SIZE);
      query.setHint("org.hibernate.readOnly", true);
      return query.getResultStream();
    });
  }

  /**
   * The type Unchecked link data.
   *
   * @param runId The ID of the run to which to add this link.
   * @param recordId The Europeana record ID in which this link is present.
   * @param recordLastIndexTime The last time this record was indexed.
   * @param recordEdmType The edm:type of the record.
   * @param recordContentTier The content tier of the record.
   * @param recordMetadataTier The metadata tier of the record.
   * @param linkUrl The actual link.
   * @param linkType The type of the link reference in the record.
   */
  public record UncheckedLinkData(long runId,
                                  String recordId,
                                  Instant recordLastIndexTime,
                                  String recordEdmType,
                                  String recordContentTier,
                                  String recordMetadataTier,
                                  String linkUrl,
                                  eu.europeana.clio.common.model.LinkType linkType) {

  }

  /**
   * The type Run with link.
   *
   * @param run The run to which the link belongs.
   * @param link The link.
   */
  public record RunWithLink(Run run, Link link) {

    /**
     * Instantiates a new Run with link.
     *
     * @param runRow the run row
     * @param linkRow the link row
     */
    public RunWithLink(RunRow runRow, LinkRow linkRow) {
      this(RunDao.convert(runRow), convert(linkRow));
    }
  }
}
