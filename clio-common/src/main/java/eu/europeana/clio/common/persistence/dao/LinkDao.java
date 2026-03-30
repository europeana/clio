package eu.europeana.clio.common.persistence.dao;

import static eu.europeana.clio.common.persistence.dao.RunDao.HUNDRED;
import static eu.europeana.clio.common.persistence.dao.RunDao.addPredicateAndParameter;
import static eu.europeana.clio.common.persistence.dao.RunDao.addPredicateAndParameterDateRange;
import static eu.europeana.clio.common.persistence.dao.RunDao.addPredicateAndParameterExcludedIds;
import static eu.europeana.clio.common.persistence.dao.RunDao.addPredicatePercentLinksInOperation;
import static eu.europeana.clio.common.persistence.dao.RunDao.buildCheckRunsQueryParts;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.dao.RunDao.QueryParts;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.LinkRow.LinkType;
import eu.europeana.clio.common.persistence.model.RunRow;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;

/**
 * Data access object for links (to be checked once as part of a run).
 */
@Slf4j
public class LinkDao {

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
   * @param uncheckedLinkData the data of the link to create.
   * This includes the run to which this link belongs,
   * the record in which this link is present, and the link URL and type.
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
    return hibernateSessionUtils.performForStream(session -> {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      QueryParts<RunWithLink> parts = buildCheckRunsQueryParts(criteriaBuilder, RunWithLink.class);
      CriteriaQuery<RunWithLink> criteriaQuery = parts.criteriaQuery();
      Root<LinkRow> link = parts.link();
      Join<LinkRow, RunRow> run = parts.run();
      Join<RunRow, DatasetRow> dataset = parts.dataset();
      Join<RunRow, BatchRow> batch = parts.batch();
      List<Predicate> predicates = parts.wherePredicates();
      List<Predicate> havingPredicates = parts.havingPredicates();
      Map<ParameterExpression<?>, Object> parametersMap = parts.parametersMap();

      // aggregations
      Expression<Long> errorsLinks = criteriaBuilder.count(link.get(FieldNames.ERROR_MESSAGE_DB));
      Expression<Long> totalLinks = criteriaBuilder.count(link);
      Expression<Integer> percentLinksInOperation = criteriaBuilder.diff(HUNDRED,
          criteriaBuilder.prod(
              criteriaBuilder.quot(
                  criteriaBuilder.toDouble(errorsLinks),
                  criteriaBuilder.toDouble(criteriaBuilder.coalesce(totalLinks, 1))
              ),
              HUNDRED
          )).cast(Integer.class);

      // wherePredicates
      addPredicateAndParameter(filters.getProvider(), criteriaBuilder, predicates, dataset, parametersMap, FieldNames.PROVIDER);
      addPredicateAndParameter(filters.getDataProvider(), criteriaBuilder, predicates, dataset, parametersMap, FieldNames.DATA_PROVIDER);
      addPredicateAndParameter(filters.getDatasetId(), criteriaBuilder, predicates, dataset, parametersMap, FieldNames.DATASET_ID);
      addPredicateAndParameter(filters.getDatasetName(), criteriaBuilder, predicates, dataset, parametersMap, FieldNames.DATASET_NAME_DB);
      addPredicateAndParameterExcludedIds(filters.getExcludedCheckId(), criteriaBuilder, predicates, run, parametersMap);
      addPredicateAndParameterDateRange(filters, criteriaBuilder, predicates, run, parametersMap);
      addPredicatePercentLinksInOperation(filters, criteriaBuilder, havingPredicates, percentLinksInOperation, parametersMap);

      // AND combination
      Predicate whereClause = criteriaBuilder.and(predicates);

      // select
      criteriaQuery.select(criteriaBuilder.construct(RunWithLink.class, run, link));

      // where
      criteriaQuery.where(whereClause);
      criteriaQuery.having(havingPredicates);

      // order by
      // ORDER BY l.run.dataset.datasetId ASC, l.recordId ASC, l.linkType ASC, l.linkUrl ASC
      criteriaQuery.orderBy(
          criteriaBuilder.asc(dataset.get(FieldNames.DATASET_ID_DB)),
          criteriaBuilder.asc(link.get(FieldNames.RECORD_ID_DB)),
          criteriaBuilder.asc(link.get(FieldNames.LINK_TYPE_DB)),
          criteriaBuilder.asc(link.get(FieldNames.LINK_URL_DB))
      );

      // group by
      criteriaQuery.groupBy(
          batch.get(FieldNames.BATCH_ID_DB),
          dataset.get(FieldNames.DATASET_ID_DB),
          link.get(FieldNames.LINK_ID_DB),
          run.get(FieldNames.RUN_ID_DB)
      );

      // create query
      TypedQuery<RunWithLink> query = session.createQuery(criteriaQuery);

      // set value to parameters
      parametersMap.forEach((key, value) -> query.setParameter(key.getName(), value));

      return query.setFirstResult(filters.getOffset())
                  .setMaxResults(filters.getLimit())
                  .getResultStream();
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
    public RunWithLink (RunRow runRow, LinkRow linkRow) {
      this(RunDao.convert(runRow), convert(linkRow));
    }
  }
}
