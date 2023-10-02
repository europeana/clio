package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.LinkRow.LinkType;
import eu.europeana.clio.common.persistence.model.RunRow;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;

/**
 * Data access object for links (to be checked once as part of a run).
 */
public class LinkDao {

  private final HibernateSessionUtils hibernateSessionUtils;

  /**
   * Constructor.
   *
   * @param sessionFactory The connection to the Clio persistence. Should be connected. This
   * object does not close the connection.
   */
  public LinkDao(SessionFactory sessionFactory) {
    this.hibernateSessionUtils = new HibernateSessionUtils(sessionFactory);
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
    final LinkType persistentLinkType;
    switch (linkType) {
      case IS_SHOWN_AT:
        persistentLinkType = LinkType.IS_SHOWN_AT;
        break;
      case IS_SHOWN_BY:
        persistentLinkType = LinkType.IS_SHOWN_BY;
        break;
      default:
        throw new IllegalStateException();
    }

    // Create and save the link
    return hibernateSessionUtils.performInTransaction(session -> {
      final RunRow runRow = session.get(RunRow.class, runId);
      if (runRow == null) {
        throw new PersistenceException(
                "Cannot create link: run with ID " + runId + " does not exist.");
      }
      final LinkRow newLink = new LinkRow(runRow, recordId, recordLastIndexTime, recordEdmType,
              recordContentTier, recordMetadataTier, persistentLinkType, linkUrl,
              computeServer(linkUrl));
      return (Long) session.save(newLink);
    });
  }

  private static String computeServer(String url) {
    try {
      final URL convertedUrl = new URL(url);
      return convertedUrl.getProtocol() + "://" + convertedUrl.getAuthority() + "/";
    } catch (MalformedURLException e) {
      return null;
    }
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
   * Update a link to add the result of the link checking for this link. This method updates all
   * unchecked links that have the same link URL (there may theoretically be multiple even though it
   * is not very likely).
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
   * This method returns all broken links that are part of a run that is the latest executed and
   * completed run for it's dataset. Essentially, this returns the current error state: for each
   * dataset it looks at the latest completed run and returns any links that are broken.
   *
   * @return A list of pairs with runs and links. The list is sorted by (Metis) dataset ID, then
   * record ID, then link type, then link URL.
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

  private static Link convert(LinkRow row) {

    // Compute the link type.
    final eu.europeana.clio.common.model.LinkType publicLinkType;
    switch (row.getLinkType()) {
      case IS_SHOWN_AT:
        publicLinkType = eu.europeana.clio.common.model.LinkType.IS_SHOWN_AT;
        break;
      case IS_SHOWN_BY:
        publicLinkType = eu.europeana.clio.common.model.LinkType.IS_SHOWN_BY;
        break;
      default:
        throw new IllegalStateException();
    }

    // Return link
    return new Link(row.getLinkId(), row.getRecordId(), row.getRecordLastIndexTime(),
            row.getRecordEdmType(), row.getRecordContentTier(), row.getRecordMetadataTier(),
            publicLinkType, row.getLinkUrl(), row.getServer(), row.getError(),
            row.getCheckingTime());
  }
}
