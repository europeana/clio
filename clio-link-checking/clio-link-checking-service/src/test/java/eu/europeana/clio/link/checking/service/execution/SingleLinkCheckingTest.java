package eu.europeana.clio.link.checking.service.execution;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SingleLinkCheckingTest {

  @Test
  void checkSingleLinkTest() throws ClioException, IOException {
    String linkToCheck = "https://example.com/";
    try (final LinkChecker linkChecker = createLinkChecker()) {
      Assertions.assertDoesNotThrow(() -> linkChecker.performLinkChecking(linkToCheck));
    }
  }

  private LinkChecker createLinkChecker() {
    final MediaProcessorFactory mediaProcessorFactory = new MediaProcessorFactory();
    mediaProcessorFactory.setResourceConnectTimeout(5000);
    mediaProcessorFactory.setResourceResponseTimeout(10000);
    mediaProcessorFactory.setResourceDownloadTimeout(30000);
    return mediaProcessorFactory.createLinkChecker();
  }
}
