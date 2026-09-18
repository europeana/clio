package eu.europeana.clio.reporting.runner.execution;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.reporting.service.ReportingEngine;
import eu.europeana.clio.reporting.service.config.ReportingEngineConfiguration;
import java.io.BufferedWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class ReportingRunnerTest {

  @TempDir
  Path tempDir;

  @Test
  void run_shouldGenerateReport_andCreateFile_whenEngineSucceeds() throws ClioException {
    // Given
    Path reportFile = tempDir.resolve("clio_report_success.txt");
    ReportingEngineConfiguration reportingConfig = mock(ReportingEngineConfiguration.class);

    try (MockedStatic<ReportingEngine> staticMock = mockStatic(ReportingEngine.class);
        MockedConstruction<ReportingEngine> mockedReporting = mockConstruction(ReportingEngine.class,
            (mock, context) -> {
              doNothing().when(mock).generateReport(any(BufferedWriter.class),any());
            })) {

      // When
      staticMock.when(ReportingEngine::getReportFileNameSuggestion).thenReturn(reportFile.toString());
      ReportingRunner runner = new ReportingRunner(reportingConfig);
      assertDoesNotThrow(() -> runner.run(new String[0]));

      // Then - check exactly one constructed instance each
      assertEquals(1, mockedReporting.constructed().size());
      ReportingEngine reportingMock = mockedReporting.constructed().getFirst();
      verify(reportingMock, times(1)).generateReport(any(BufferedWriter.class), any());
      assertTrue(Files.exists(reportFile));
    }
  }


  @ParameterizedTest
  @ValueSource(classes = {ClioException.class, RuntimeException.class})
  void run_shouldCatchClioException_andLogWarning_whenEngineThrows(Class<? extends Exception> clazz)
      throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    // Given
    Path reportFile = tempDir.resolve("clio_report_exception.txt");
    ReportingEngineConfiguration reportingConfig = mock(ReportingEngineConfiguration.class);
    Exception testException = clazz.getConstructor().newInstance();
    try (MockedStatic<ReportingEngine> staticMock = mockStatic(ReportingEngine.class);
        MockedConstruction<ReportingEngine> mockedReporting = mockConstruction(ReportingEngine.class,
            (mock, context) -> {
              doThrow(testException).when(mock)
                                    .generateReport(any(BufferedWriter.class), any());
            })) {

      // When
      staticMock.when(ReportingEngine::getReportFileNameSuggestion).thenReturn(reportFile.toString());
      ReportingRunner runner = new ReportingRunner(reportingConfig);
      assertDoesNotThrow(() -> runner.run(new String[0]));

      // Then - should not throw, exception is caught and logged
      assertEquals(1, mockedReporting.constructed().size());
      assertTrue(Files.exists(reportFile));
    }
  }
}
