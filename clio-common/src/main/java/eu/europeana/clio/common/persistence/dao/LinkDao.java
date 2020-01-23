package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection;
import eu.europeana.clio.common.persistence.ClioPersistenceConnection.Result;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.LinkRow.LinkType;
import eu.europeana.clio.common.persistence.model.RunRow;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Data access object for links (to be checked once as part of a run).
 */
public class LinkDao {

  private final ClioPersistenceConnection persistenceConnection;

  /**
   * Constructor.
   *
   * @param persistenceConnection The connection to the Clio persistence. Should be connected. This
   * object does not close the connection.
   */
  public LinkDao(ClioPersistenceConnection persistenceConnection) {
    this.persistenceConnection = persistenceConnection;
  }

  /**
   * Create (i.e. persist) a link that is not yet checked by Clio.
   *
   * @param runId The ID of the run to which to add this link.
   * @param recordId The Europeana record ID in which this link is present.
   * @param linkUrl The actual link.
   * @param linkType The type of the link reference in the record.
   * @return The ID of the link.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public long createUncheckedLink(long runId, String recordId, String linkUrl,
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
    return persistenceConnection.performInTransaction(session -> {
      final RunRow runRow = session.get(RunRow.class, runId);
      if (runRow == null) {
        throw new PersistenceException(
                "Cannot create link: run with ID " + runId + " does not exist.");
      }
      final LinkRow newLink = new LinkRow(runRow, recordId, persistentLinkType, linkUrl,
              computeServer(linkUrl));
      return (long) session.save(newLink);
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
  public Result<Link> getAllUncheckedLinks() throws PersistenceException {
    return persistenceConnection.performForStream(
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
    final long checkingTime = System.currentTimeMillis();
    persistenceConnection.performInTransaction(session -> {
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
   * @return A map with the runs as keys and the list of associated links as values. The map is
   * sorted by (Metis) dataset ID.
   * @throws PersistenceException In case there was a persistence problem.
   */
  public SortedMap<Run, List<Link>> getBrokenLinksInLatestCompletedRuns()
          throws PersistenceException {
    final List<LinkRow> links = persistenceConnection.performInSession(session -> session
            .createNamedQuery(LinkRow.GET_BROKEN_LINKS_IN_LATEST_COMPLETED_RUNS, LinkRow.class)
            .getResultList());
    return links.stream().collect(Collectors.groupingBy(link -> RunDao.convert(link.getRun()),
            () -> new TreeMap<>(Comparator.comparing(run -> run.getDataset().getDatasetId())),
            Collectors.mapping(LinkDao::convert, Collectors.toList())));
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
    final Instant checkingTime = Optional.ofNullable(row.getCheckingTime())
            .map(Instant::ofEpochMilli).orElse(null);
    return new Link(row.getLinkId(), row.getRecordId(), publicLinkType, row.getLinkUrl(),
            row.getServer(), row.getError(), checkingTime);
  }
}
