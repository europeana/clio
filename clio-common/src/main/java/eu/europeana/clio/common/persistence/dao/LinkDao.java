package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.LinkRow.LinkType;
import eu.europeana.clio.common.persistence.model.RunRow;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;

/**
 * Data access object for links (to be checked once as part of a run).
 */
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
      final URL convertedUrl = new URL(url);
      return convertedUrl.getProtocol() + "://" + convertedUrl.getAuthority() + "/";
    } catch (MalformedURLException e) {
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
   * @param runId The ID of the run to which to add this link.
   * @param recordId The Europeana record ID in which this link is present.
   * @param recordLastIndexTime The last time this record was indexed.
   * @param recordEdmType The edm:type of the record.
   * @param recordContentTier The content tier of the record.
   * @param recordMetadataTier The metadata tier of the record.
   * @param linkUrl The actual link.
   * @param linkType The type of the link reference in the record.
   * @return The ID of the link.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public long createUncheckedLink(long runId, String recordId, Instant recordLastIndexTime,
      String recordEdmType, String recordContentTier, String recordMetadataTier, String linkUrl,
      eu.europeana.clio.common.model.LinkType linkType) throws PersistenceException {

    // Compute the link type.
    final LinkType persistentLinkType = switch (linkType) {
      case IS_SHOWN_AT -> LinkType.IS_SHOWN_AT;
      case IS_SHOWN_BY -> LinkType.IS_SHOWN_BY;
      default -> throw new IllegalStateException();
    };

    // Create and save the link
    return hibernateSessionUtils.performInTransaction(session -> {
      final RunRow runRow = session.find(RunRow.class, runId);
      if (runRow == null) {
        throw new PersistenceException(
            "Cannot create link: run with ID " + runId + " does not exist.");
      }
      final LinkRow newLink = new LinkRow(runRow, recordId, recordLastIndexTime, recordEdmType,
          recordContentTier, recordMetadataTier, persistentLinkType, linkUrl,
          computeServer(linkUrl));

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
  public StreamResult<Pair<Run, Link>> getBrokenLinksInLatestCompletedRuns()
      throws PersistenceException {
    return hibernateSessionUtils.performForStream(session -> session
        .createNamedQuery(LinkRow.GET_BROKEN_LINKS_IN_LATEST_COMPLETED_RUNS, LinkRow.class)
        .getResultStream()
        .map(link -> new ImmutablePair<>(RunDao.convert(link.getRun()), convert(link)))
    );
  }

  /**
   * Gets links with runs for filters.
   *
   * @param filters the filters
   * @return the links with runs for filters
   * @throws PersistenceException the persistence exception
   */
  public StreamResult<Pair<Run, Link>> getLinksWithRunsForFilters(FieldFilters filters) throws PersistenceException {
    return hibernateSessionUtils.performForStream(session -> {
      CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
      CriteriaQuery<Tuple> criteriaQuery = criteriaBuilder.createTupleQuery();

      Root<LinkRow> link = criteriaQuery.from(LinkRow.class);
      // joins
      Join<LinkRow, RunRow> run = link.join("run", JoinType.INNER);
      Join<RunRow, DatasetRow> dataset = run.join("dataset", JoinType.INNER);
      Join<RunRow, BatchRow> batch = run.join("batch", JoinType.INNER);

      List<Predicate> predicates = new ArrayList<>();
      HashMap<ParameterExpression<?>, Object> parametersMap = new HashMap<>();

      // predicates
      if (!(filters.getProvider() == null || filters.getProvider().isEmpty())) {
        ParameterExpression<Set> providersParameter = criteriaBuilder.parameter(Set.class, "providers");
        predicates.add(dataset.get("provider").in(providersParameter));
        parametersMap.put(providersParameter, filters.getProvider());
      }

      if (!(filters.getDataProvider() == null || filters.getDataProvider().isEmpty())) {
        ParameterExpression<Set> dataProvidersParameter = criteriaBuilder.parameter(Set.class, "dataProviders");
        predicates.add(dataset.get("dataProvider").in(dataProvidersParameter));
        parametersMap.put(dataProvidersParameter, filters.getDataProvider());
      }

      if (!(filters.getDatasetId() == null || filters.getDatasetId().isEmpty())) {
        ParameterExpression<Set> datasetIdParameter = criteriaBuilder.parameter(Set.class, "datasetIds");
        predicates.add(dataset.get("datasetId").in(datasetIdParameter));
        parametersMap.put(datasetIdParameter, filters.getDatasetId());
      }

      if (!(filters.getDatasetName() == null || filters.getDatasetName().isEmpty())) {
        ParameterExpression<Set> datasetNameParameter = criteriaBuilder.parameter(Set.class, "datasetName");
        predicates.add(dataset.get("name").in(datasetNameParameter));
        parametersMap.put(datasetNameParameter, filters.getDatasetName());
      }

      if (!(filters.getExcludedCheckIds() == null || filters.getExcludedCheckIds().isEmpty())) {
        ParameterExpression<Set> excludeCheckIdsParameter = criteriaBuilder.parameter(Set.class, "excludedCheckIds");
        predicates.add(criteriaBuilder.not(run.get("runId").in(excludeCheckIdsParameter)));
        parametersMap.put(excludeCheckIdsParameter, filters.getExcludedCheckIds());
      }

      if (filters.getDateFrom() != null) {
        ParameterExpression<Long> dateFromParameter = criteriaBuilder.parameter(Long.class, "startingTime");
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(run.get("startingTime"), dateFromParameter));
        parametersMap.put(dateFromParameter, filters.getDateFrom().toInstant().toEpochMilli());
      }

      if (filters.getDateTo() != null) {
        ParameterExpression<Long> dateToParameter = criteriaBuilder.parameter(Long.class, "endTime");
        predicates.add(criteriaBuilder.lessThanOrEqualTo(run.get("startingTime"), dateToParameter));
        Duration addEndOfDay = Duration.ofHours(23)
                                       .plusMinutes(59)
                                       .plusSeconds(59);
        parametersMap.put(dateToParameter, filters.getDateTo().toInstant().plus(addEndOfDay).toEpochMilli());
      }

      // OR combination
      Predicate whereClause = criteriaBuilder.and(predicates);

      // aggregations
      Expression<Long> errorsLinks = criteriaBuilder.count(link.get("error"));
      Expression<Long> totalLinks = criteriaBuilder.count(run.get("runId"));
      Expression<Long> startingTime = criteriaBuilder.min(run.get("startingTime"));

      // select
      criteriaQuery.select(criteriaBuilder.tuple(
          link,
          run,
          dataset,
          startingTime.alias("startingTime"),
          errorsLinks.alias("errorsLinks"),
          totalLinks.alias("totalLinks")
      ));

      // where
      criteriaQuery.where(whereClause);

      // order by
      // ORDER BY l.run.dataset.datasetId ASC, l.recordId ASC, l.linkType ASC, l.linkUrl ASC
      criteriaQuery.orderBy(
          criteriaBuilder.asc(dataset.get("datasetId")),
          criteriaBuilder.asc(link.get("recordId")),
          criteriaBuilder.asc(link.get("linkType")),
          criteriaBuilder.asc(link.get("linkUrl"))
      );

      // group by
      criteriaQuery.groupBy(
          batch.get("batchId"),
          dataset.get("datasetId"),
          link.get("linkId"),
          run.get("runId")
      );

      // create query
      TypedQuery<Tuple> query = session.createQuery(criteriaQuery);

      // set value to parameters
      parametersMap.forEach((key, value) -> query.setParameter(key.getName(), value));

      return query
          .getResultStream()
          .filter(tuple ->
              (filters.getPercentLinksInOperationFrom() == null)
                  || ((long) tuple.get("errorsLinks") * 100 / (long) tuple.get("totalLinks"))
                  >= filters.getPercentLinksInOperationFrom()
                  && (filters.getPercentLinksInOperationTo() == null)
                  || ((long) tuple.get("errorsLinks") * 100 / (long) tuple.get("totalLinks"))
                  <= filters.getPercentLinksInOperationTo())
          .map(tuple -> new ImmutablePair<>(RunDao.convert((RunRow) tuple.get(1)), convert((LinkRow) tuple.get(0))));
    });
  }

  public record LinkWithRun(Link link, Run run) {

  }
}
