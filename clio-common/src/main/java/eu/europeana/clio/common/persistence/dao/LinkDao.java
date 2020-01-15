package eu.europeana.clio.common.persistence.dao;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.persistence.ConnectionProvider;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.LinkRow.LinkType;
import eu.europeana.clio.common.persistence.model.RunRow;
import java.net.URL;

public class LinkDao {

  private final ConnectionProvider connectionProvider;

  public LinkDao(ConnectionProvider connectionProvider) {
    this.connectionProvider = connectionProvider;
  }

  public long createUncheckedLink(long runId, String recordId, URL linkUrl,
          LinkType linkType) throws PersistenceException {

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

    // Compute the server (i.e. the base URL
    final String server = linkUrl.getProtocol() + "://" + linkUrl.getAuthority() + "/";

    // Create and save the link
    return connectionProvider.performInTransaction(session -> {
      final RunRow runRow = session.get(RunRow.class, runId);
      if (runRow == null) {
        throw new PersistenceException(
                "Cannot create link: run with ID " + runId + " does not exist.");
      }
      final LinkRow newLink = new LinkRow(runRow, recordId, persistentLinkType, linkUrl.toString(),
              server);
      return (long) session.save(newLink);
    });
  }
}
