package eu.europeana.clio.linkchecking;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.linkchecking.config.ConfigurationPropertiesHolder;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.exception.LinkCheckingException;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

/**
 * This class is an entry point of the link checking module of Clio that checks just one link
 * (passed as the first argument). It can be used for testing purposes to check the behavior of this
 * module without having to run a whole link checking execution. It contains a main method ({@link
 * #run(String[])}) that can be used to trigger the functionality.
 *
 * <p>To enable this run, replace the CommandLineRunner bean</p>
 */
public class CheckSingleLinkRunner implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckSingleLinkRunner.class);

  private final ConfigurationPropertiesHolder propertiesHolder;

  public CheckSingleLinkRunner(ConfigurationPropertiesHolder propertiesHolder) {
    this.propertiesHolder = propertiesHolder;
  }

  /**
   * Main method.
   *
   * @param args The input arguments.
   */
  @Override
  public void run(String... args) {
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

  private void mainInternal(String linkToCheck) throws ClioException {
    // Perform link checking
    LOGGER.info("Checking link: {}", linkToCheck);
    try (final LinkChecker linkChecker = propertiesHolder.createLinkChecker()) {
      // Return something so that we can add annotation.
      // See https://github.com/spotbugs/spotbugs/issues/756
      @SuppressWarnings("findbugs:RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
      final String message = performLinkChecking(linkChecker, linkToCheck);
      LOGGER.info(message);
    } catch (LinkCheckingException e) {
      LOGGER.warn("Link checking failed on the link.", e);
    } catch (IOException e) {
      throw new ClioException("Could not close link checker.", e);
    }
  }

  private static String performLinkChecking(LinkChecker linkChecker, String linkToCheck)
          throws LinkCheckingException {
    linkChecker.performLinkChecking(linkToCheck);
    return "Link checking successful.";
  }
}
