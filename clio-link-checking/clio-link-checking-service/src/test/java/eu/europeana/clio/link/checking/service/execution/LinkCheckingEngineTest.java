package eu.europeana.clio.link.checking.service.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.dao.BatchDao;
import eu.europeana.clio.common.persistence.dao.LinkDao;
import eu.europeana.clio.link.checking.service.config.LinkCheckingEngineConfiguration;
import eu.europeana.clio.link.checking.service.config.properties.LinkCheckingConfigurationProperties;
import eu.europeana.metis.mediaprocessing.LinkChecker;
import eu.europeana.metis.mediaprocessing.exception.LinkCheckingException;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkCheckingEngineTest {

  private LinkCheckingEngineConfiguration config;

  @Test
  void performLinkCheckingOnAllUncheckedLinks() throws ClioException, LinkCheckingException {
    // Given
    config = mock(LinkCheckingEngineConfiguration.class);
    LinkCheckingConfigurationProperties props = mock(LinkCheckingConfigurationProperties.class);
    when(props.getRunExecuteThreads()).thenReturn(1);
    when(config.getLinkCheckingConfigurationProperties()).thenReturn(props);
    SessionFactory sessionFactory = mock(SessionFactory.class);
    when(config.getSessionFactory()).thenReturn(sessionFactory);

    LinkChecker checker = mock(LinkChecker.class);
    doThrow(new LinkCheckingException("bad", new RuntimeException()))
        .when(checker)
        .performLinkChecking("http://bad-linkUrl");
    doNothing()
        .when(checker)
        .performLinkChecking("http://good-linkUrl");
    when(config.createLinkChecker()).thenReturn(checker);

    // When
    LinkCheckingEngine engine = new LinkCheckingEngine(config);
    try (MockedConstruction<LinkDao> mocked = Mockito.mockConstruction(LinkDao.class,
        (mock, context) -> {
          StreamResult<Link> streamResult = mock(StreamResult.class);
          Link link1 = mock(Link.class);
          when(link1.getServer()).thenReturn("server-bad");
          when(link1.getLinkUrl()).thenReturn("http://bad-linkUrl");

          Link link2 = mock(Link.class);
          when(link2.getServer()).thenReturn("server-good");
          when(link2.getLinkUrl()).thenReturn("http://good-linkUrl");
          when(streamResult.get()).thenReturn(List.of(link1, link2).stream());
          when(mock.getAllUncheckedLinks()).thenReturn(streamResult);
        })) {
      engine.performLinkCheckingOnAllUncheckedLinks();
      assertEquals(1, mocked.constructed().size());
      LinkDao created = mocked.constructed().getFirst();
      verify(created, times(1)).getAllUncheckedLinks();
      verify(created, times(1)).registerLinkChecking(eq("http://bad-linkUrl"), anyString());
      verify(created, times(1)).registerLinkChecking(eq("http://good-linkUrl"), isNull());
    }

    // Then
    verify(config, times(1)).createLinkChecker();
    verify(config, times(1)).getSessionFactory();
    verify(checker, times(1)).performLinkChecking("http://bad-linkUrl");
    verify(checker, times(1)).performLinkChecking("http://good-linkUrl");
  }

  @Test
  void removeOldData_callsBatchDaoDeleteOlderBatches() throws Exception {
    // Given
    config = mock(LinkCheckingEngineConfiguration.class);
    when(config.getSessionFactory()).thenReturn(null);
    LinkCheckingConfigurationProperties props = mock(LinkCheckingConfigurationProperties.class);
    when(props.getRetentionMonths()).thenReturn(1);
    when(config.getLinkCheckingConfigurationProperties()).thenReturn(props);
    // Mock construction of BatchDao so the real DB code is never executed
    try (MockedConstruction<BatchDao> mocked = Mockito.mockConstruction(BatchDao.class,
        (mock, context) -> {
          // do nothing; will verify call afterwards
        })) {

      // When
      LinkCheckingEngine engine = new LinkCheckingEngine(config);
      engine.removeOldData();

      // Then verify that a BatchDao was constructed and deleteOlderBatches called once
      assertEquals(1, mocked.constructed().size());
      BatchDao created = mocked.constructed().getFirst();
      verify(created, times(1)).deleteOlderBatches(anyInt());
    }
  }
}

