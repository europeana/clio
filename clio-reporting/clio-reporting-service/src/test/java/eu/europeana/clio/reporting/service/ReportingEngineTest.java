package eu.europeana.clio.reporting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.config.properties.ClioConfigurationProperties;
import eu.europeana.clio.common.model.BatchWithCounters;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.LinkType;
import eu.europeana.clio.common.model.Report;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.dao.BatchDao;
import eu.europeana.clio.common.persistence.dao.LinkDao;
import eu.europeana.clio.common.persistence.dao.ReportDao;
import eu.europeana.clio.reporting.service.config.ReportingEngineConfiguration;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class ReportingEngineTest {

  @Test
  void generateReport_writesHeaderAndRow() throws Exception {
    // Given
    ReportingEngineConfiguration config = mock(ReportingEngineConfiguration.class);
    ClioConfigurationProperties clioConfig = mock(ClioConfigurationProperties.class);
    when(config.clioConfigurationProperties()).thenReturn(clioConfig);
    when(config.clioConfigurationProperties().datasetReportLinkTemplate()).thenReturn("http://example.com/datasets/%s");
    StreamResult<Pair<Run, Link>> streamResult = mock(StreamResult.class);

    Run run = mock(Run.class);
    Dataset dataset = mock(Dataset.class);
    when(run.getDataset()).thenReturn(dataset);
    when(dataset.getDatasetId()).thenReturn("Dataset1");
    when(dataset.getSize()).thenReturn(42);
    when(dataset.getProvider()).thenReturn("provider");
    when(dataset.getDataProvider()).thenReturn("dataProvider");

    Link link = mock(Link.class);
    when(link.getRecordId()).thenReturn("record1");
    when(link.getRecordLastIndexTime()).thenReturn(Instant.ofEpochMilli(1000));
    when(link.getRecordEdmType()).thenReturn("edmType");
    when(link.getRecordContentTier()).thenReturn("contentTier");
    when(link.getRecordMetadataTier()).thenReturn("metadataTier");
    LinkType linkType = mock(LinkType.class);
    when(linkType.getHumanReadableName()).thenReturn("LinkType");
    when(link.getLinkType()).thenReturn(linkType);
    when(link.getLinkUrl()).thenReturn("http://broken");
    when(link.getServer()).thenReturn("server1");
    when(link.getCheckingTime()).thenReturn(Instant.ofEpochMilli(2000));
    when(link.getError()).thenReturn("404");
    when(streamResult.get()).thenReturn(Stream.of(Pair.of(run, link)));

    try (MockedConstruction<LinkDao> ignored = mockConstruction(LinkDao.class,
        (mock, context) -> when(mock.getBrokenLinksInLatestCompletedRuns()).thenReturn(streamResult))) {

      ReportingEngine engine = new ReportingEngine(config);
      StringWriter sw = new StringWriter();
      // When
      engine.generateReport(sw);
      String out = sw.toString();
      // Then
      assertTrue(out.contains("Dataset ID"), "CSV header must be present");
      assertTrue(out.contains("Dataset1"), "Dataset id must be present");
      assertTrue(out.contains("http://example.com/datasets/Dataset1"), "Dataset link must be formatted");
      assertTrue(out.contains("record1"), "Record id must be present");
      assertTrue(out.contains("http://broken"), "Broken link must be present");
      assertTrue(out.contains("404"), "Error must be present");
    }
  }

  @Test
  void storeReport_savesReportWhenLatestBatchExists() throws Exception {
    // Given
    ReportingEngineConfiguration config = mock(ReportingEngineConfiguration.class);
    BatchWithCounters batchWithCounters = mock(BatchWithCounters.class);
    when(batchWithCounters.getBatchId()).thenReturn(123L);

    try (MockedConstruction<BatchDao> batchCtor = mockConstruction(BatchDao.class,
        (mock, context) -> when(mock.getLatestBatches(1)).thenReturn(List.of(batchWithCounters)));
        MockedConstruction<ReportDao> reportCtor = mockConstruction(ReportDao.class)) {

      ReportingEngine engine = new ReportingEngine(config);
      // When
      engine.storeReport("the-report");

      // Then verify ReportDao.saveReport called with expected batch id
      ReportDao createdReportDao = reportCtor.constructed().getFirst();
      verify(createdReportDao).saveReport("the-report", 123L);
    }
  }

  @Test
  void dao_proxy_methods_returnDaoResults() throws Exception {
    ReportingEngineConfiguration config = mock(ReportingEngineConfiguration.class);

    List<BatchWithCounters> batches = List.of(mock(BatchWithCounters.class));
    List<Report> reports = List.of(mock(Report.class));

    try (MockedConstruction<BatchDao> batchCtor = mockConstruction(BatchDao.class,
        (mock, context) -> when(mock.getLatestBatches(5)).thenReturn(batches));
        MockedConstruction<ReportDao> reportCtor = mockConstruction(ReportDao.class,
            (mock, context) -> {
              when(mock.getLatestReports(10)).thenReturn(reports);
              when(mock.getAllReportDetails()).thenReturn(reports);
              when(mock.getReport(7L)).thenReturn(reports.getFirst());
            })) {

      ReportingEngine engine = new ReportingEngine(config);

      assertEquals(batches, engine.getLatestBatches(5));
      assertEquals(reports, engine.getLatestReports(10));
      assertEquals(reports, engine.getAllReportDetails());
      assertEquals(reports.getFirst(), engine.getReportByBatchId(7L));
    }
  }

  @Test
  void fileNameSuggestions_formatCorrectly() {
    String suggested = ReportingEngine.getReportFileNameSuggestion();
    assertTrue(suggested.startsWith("clio_report_"));
    assertTrue(suggested.endsWith(".csv"));

    Report report = mock(Report.class);
    when(report.getBatchId()).thenReturn(55L);
    when(report.getCreationTime()).thenReturn(Instant.now().toEpochMilli());

    String suggested2 = ReportingEngine.getReportFileNameSuggestion(report);
    assertTrue(suggested2.contains("_55_"));
    assertTrue(suggested2.endsWith(".csv"));
  }
}
