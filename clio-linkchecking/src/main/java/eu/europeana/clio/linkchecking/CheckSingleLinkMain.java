package eu.europeana.clio.linkchecking;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.linkchecking.config.PropertiesHolder;
import eu.europeana.clio.linkchecking.config.TruststoreInitializer;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.exception.LinkCheckingException;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an entry point of the link checking module of Clio that checks just one link
 * (passed as the first argument). It can be used for testing purposes to check the behavior of this
 * module without having to run a whole link checking execution. It contains a main method ({@link
 * #main(String[])}) that can be used to trigger the functionality.
 */
public class CheckSingleLinkMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckSingleLinkMain.class);

  /**
   * Main method.
   *
   * @param args The input arguments.
   */
  public static void main(String[] args) {
    try {
      final String linkToCheck = Optional.of(args).filter(list -> list.length > 0)
              .map(list -> list[0])
              .orElseThrow(() -> new IllegalArgumentException("Provide a link to check."));
      mainInternal(linkToCheck);
    } catch (ClioException | RuntimeException e) {
      LOGGER.warn("Something unexpected happened while performing link checking.", e);
      System.exit(1);
    }
  }

  private static void mainInternal(String linkToCheck) throws ClioException {

    // Read the properties and initialize
    final PropertiesHolder properties = new PropertiesHolder();
    TruststoreInitializer.initializeTruststore(properties);

    // Perform link checking
    LOGGER.info("Checking link: {}", linkToCheck);
    try (final LinkChecker linkChecker = properties.createLinkChecker()) {
      linkChecker.performLinkChecking(linkToCheck);
      LOGGER.info("Link checking successful.");
    } catch (LinkCheckingException e) {
      LOGGER.warn("Link checking failed on the link.", e);
    } catch (IOException e) {
      throw new ClioException("Could not close link checker.", e);
    }
  }
}
