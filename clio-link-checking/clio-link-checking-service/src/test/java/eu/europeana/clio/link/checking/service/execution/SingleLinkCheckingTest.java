package eu.europeana.clio.link.checking.service.execution;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.exception.ConfigurationException;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.MediaProcessorFactory;
import eu.europeana.metis.mediaprocessing.exception.MediaProcessorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class SingleLinkCheckingTest {
    @Test
    void checkSingleLinkTest() throws ClioException, IOException {
        String linkToCheck = "https://example.com/";
        try (final LinkChecker linkChecker = createLinkChecker()) {
            Assertions.assertDoesNotThrow(() -> linkChecker.performLinkChecking(linkToCheck));
        }
    }

    private LinkChecker createLinkChecker() throws ConfigurationException {
        final MediaProcessorFactory mediaProcessorFactory = new MediaProcessorFactory();
        mediaProcessorFactory.setResourceConnectTimeout(5000);
        mediaProcessorFactory.setResourceResponseTimeout(10000);
        mediaProcessorFactory.setResourceDownloadTimeout(30000);
        try {
            return mediaProcessorFactory.createLinkChecker();
        } catch (MediaProcessorException e) {
            throw new ConfigurationException("Could not create link checker.", e);
        }
    }
}
