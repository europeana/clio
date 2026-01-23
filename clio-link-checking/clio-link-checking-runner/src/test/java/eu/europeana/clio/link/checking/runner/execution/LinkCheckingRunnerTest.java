package eu.europeana.clio.link.checking.runner.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.link.checking.service.config.properties.LinkCheckingConfigurationProperties;
import eu.europeana.clio.link.checking.service.config.LinkCheckingEngineConfiguration;
import eu.europeana.clio.link.checking.service.config.Mode;
import eu.europeana.clio.link.checking.service.execution.LinkCheckingEngine;
import eu.europeana.clio.reporting.service.ReportingEngine;
import eu.europeana.clio.reporting.service.config.ReportingEngineConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class LinkCheckingRunnerTest {

  @Test
  void run_shouldSkipCreatingRuns_whenModeIsLinkCheckingOnly() throws ClioException {
    // Given
    LinkCheckingEngineConfiguration engineConfig = mock(LinkCheckingEngineConfiguration.class);
    LinkCheckingConfigurationProperties props = mock(LinkCheckingConfigurationProperties.class);
    when(engineConfig.getLinkCheckingConfigurationProperties()).thenReturn(props);
    when(props.getCheckingMode()).thenReturn(Mode.LINK_CHECKING_ONLY);

    ReportingEngineConfiguration reportingConfig = mock(ReportingEngineConfiguration.class);

    try (MockedConstruction<LinkCheckingEngine> mockedEngine = mockConstruction(LinkCheckingEngine.class);
        MockedConstruction<ReportingEngine> mockedReporting = mockConstruction(ReportingEngine.class,
            (mock, context) ->
                when(mock.generateReport()).thenReturn("report of link checking only mode"))) {

      // When
      LinkCheckingRunner runner = new LinkCheckingRunner(engineConfig, reportingConfig);
      runner.run(new String[0]);

      // Then - check exactly one constructed instance each
      assertEquals(1, mockedEngine.constructed().size());
      assertEquals(1, mockedReporting.constructed().size());

      LinkCheckingEngine engineMock = mockedEngine.constructed().getFirst();
      verify(engineMock, times(1)).removeOldData();
      verify(engineMock, never()).createRunsForAllAvailableDatasets();
      verify(engineMock, times(1)).performLinkCheckingOnAllUncheckedLinks();

      ReportingEngine reportingMock = mockedReporting.constructed().getFirst();
      verify(reportingMock, times(1)).generateReport();
      verify(reportingMock, times(1)).storeReport("report of link checking only mode");
    }
  }

  @Test
  void run_shouldCreateRuns_whenModeIsFullProcessing() throws ClioException {
    // Given
    LinkCheckingEngineConfiguration engineConfig = mock(LinkCheckingEngineConfiguration.class);
    LinkCheckingConfigurationProperties props = mock(LinkCheckingConfigurationProperties.class);
    when(engineConfig.getLinkCheckingConfigurationProperties()).thenReturn(props);
    when(props.getCheckingMode()).thenReturn(Mode.FULL_PROCESSING);

    ReportingEngineConfiguration reportingConfig = mock(ReportingEngineConfiguration.class);

    try (MockedConstruction<LinkCheckingEngine> mockedEngine = mockConstruction(LinkCheckingEngine.class);
        MockedConstruction<ReportingEngine> mockedReporting = mockConstruction(ReportingEngine.class,
            (mock, context) ->
                when(mock.generateReport()).thenReturn("report of full processing mode"))) {

      // When
      LinkCheckingRunner runner = new LinkCheckingRunner(engineConfig, reportingConfig);
      runner.run(new String[0]);

      // Then
      assertEquals(1, mockedEngine.constructed().size());
      assertEquals(1, mockedReporting.constructed().size());

      LinkCheckingEngine engineMock = mockedEngine.constructed().getFirst();
      verify(engineMock, times(1)).removeOldData();
      verify(engineMock, times(1)).createRunsForAllAvailableDatasets();
      verify(engineMock, times(1)).performLinkCheckingOnAllUncheckedLinks();

      ReportingEngine reportingMock = mockedReporting.constructed().getFirst();
      verify(reportingMock, times(1)).generateReport();
      verify(reportingMock, times(1)).storeReport("report of full processing mode");
    }
  }
}
