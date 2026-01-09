package eu.europeana.clio.reporting.rest.controller;

import static eu.europeana.clio.reporting.rest.controller.ReportingController.BATCHES_ENDPOINT_PATH;
import static eu.europeana.clio.reporting.rest.controller.ReportingController.LATEST_REPORT_ENDPOINT_PATH;
import static eu.europeana.clio.reporting.rest.controller.ReportingController.REPORT_BY_BATCH_ID_ENDPOINT_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.exception.ReportNotFoundException;
import eu.europeana.clio.common.model.BatchWithCounters;
import eu.europeana.clio.common.model.Report;
import eu.europeana.clio.reporting.service.ReportingEngine;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReportingControllerTest {

  private ReportingEngine reportingEngine;
  private ReportingController controller;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    reportingEngine = mock(ReportingEngine.class);
    controller = new ReportingController(reportingEngine);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void availableReports_returnsList() throws Exception {
    Report report = mock(Report.class);
    when(report.getCreationTime()).thenReturn(1L);
    when(report.getBatchId()).thenReturn(42L);
    when(report.getReportId()).thenReturn(1L);
    when(reportingEngine.getAllReportDetails()).thenReturn(List.of(report));

    mockMvc.perform(get("/" + ReportingController.AVAILABLE_REPORTS_ENDPOINT_PATH))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith("application/json"))
           .andExpect(jsonPath("$[0].reportId").value(1))
           .andExpect(jsonPath("$[0].batchId").value(42))
           .andExpect(jsonPath("$[0].url").value(
               containsString("/" + REPORT_BY_BATCH_ID_ENDPOINT_PATH)));
  }

  @Test
  void availableReports_whenEmpty_returnsEmptyList() throws Exception {
    when(reportingEngine.getAllReportDetails()).thenReturn(Collections.emptyList());
    mockMvc.perform(get("/" + ReportingController.AVAILABLE_REPORTS_ENDPOINT_PATH))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith("application/json"))
           .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void getReportByBatchId_returnsReportBytes_andHeaders() throws Exception {
    long batchId = 25L;
    Report report = mock(Report.class);
    String content = "column1,column2\nvalue1,value2\n";
    when(reportingEngine.getReportByBatchId(batchId)).thenReturn(report);
    when(report.getReportString()).thenReturn(content);
    when(report.getBatchId()).thenReturn(batchId);

    mockMvc.perform(get("/" + REPORT_BY_BATCH_ID_ENDPOINT_PATH)
               .param(ReportingController.BATCH_ID_ENDPOINT_PARAMETER,
                   String.valueOf(batchId)))
           .andExpect(status().isOk())
           .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
           .andExpect(content().bytes(content.getBytes()));
  }

  @Test
  void getReportByBatchId_whenNull_throwsReportNotFoundException() throws PersistenceException {
    long batchId = 999L;
    when(reportingEngine.getReportByBatchId(batchId)).thenReturn(null);

    ReportNotFoundException notFoundException = assertThrows(ReportNotFoundException.class,
        () -> controller.getReportByBatchId(batchId));
    assertNull(notFoundException.getMessage());
  }

  @Test
  void getLatestReport_returnsReportBytes_andHeaders() throws Exception {
    Report latestReport = mock(Report.class);
    String content = "column1,column2\nvalue1,value2\n";
    when(reportingEngine.getLatestReports(1)).thenReturn(List.of(latestReport));
    when(latestReport.getReportString()).thenReturn(content);
    when(latestReport.getBatchId()).thenReturn(77L);

    mockMvc.perform(get("/" + LATEST_REPORT_ENDPOINT_PATH))
           .andExpect(status().isOk())
           .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
           .andExpect(content().bytes(content.getBytes()));
  }

  @Test
  void getLatestReport_whenNoReports_throwsReportNotFoundException() throws PersistenceException {
    when(reportingEngine.getLatestReports(1)).thenReturn(Collections.emptyList());
    ReportNotFoundException notFoundException = assertThrows(ReportNotFoundException.class,
        () -> controller.getLatestReport());
    assertNull(notFoundException.getMessage());
  }

  @Test
  void getBatches_whenGetLatestBatches_returnsOK() throws Exception {
    BatchWithCounters batchMock = mock(BatchWithCounters.class);
    Instant batchTimestamp = Instant.now();
    String expectedTimestamp = batchTimestamp.atZone(ZoneId.systemDefault())
                                             .toOffsetDateTime()
                                             .toString();
    when(batchMock.getBatchId()).thenReturn(123L);
    when(batchMock.getDatasetsExcludedAlreadyRunning()).thenReturn(2);
    when(batchMock.getDatasetsExcludedNotIndexed()).thenReturn(4);
    when(batchMock.getDatasetsExcludedWithoutLinks()).thenReturn(6);
    when(batchMock.getDatasetsPending()).thenReturn(8);
    when(batchMock.getDatasetsProcessed()).thenReturn(42);
    when(batchMock.getCreationTime()).thenReturn(batchTimestamp);
    when(batchMock.getLastUpdateTimeInSolr()).thenReturn(batchTimestamp);
    when(batchMock.getLastUpdateTimeInMetisCore()).thenReturn(batchTimestamp);
    when(reportingEngine.getLatestBatches(5)).thenReturn(List.of(batchMock));
    mockMvc.perform(get("/" + BATCHES_ENDPOINT_PATH).param("maxResults", "5"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith("application/json"))
           .andExpect(jsonPath("$[0].creationTime").value(expectedTimestamp))
           .andExpect(jsonPath("$[0].lastUpdateTimeInSolr").value(expectedTimestamp))
           .andExpect(jsonPath("$[0].lastUpdateTimeInMetisCore").value(expectedTimestamp))
           .andExpect(jsonPath("$[0].datasetsExcludedAlreadyRunning").value(2))
           .andExpect(jsonPath("$[0].datasetsExcludedNotIndexed").value(4))
           .andExpect(jsonPath("$[0].datasetsExcludedWithoutLinks").value(6))
           .andExpect(jsonPath("$[0].datasetsProcessed").value(42))
           .andExpect(jsonPath("$[0].datasetsPending").value(8));
  }

  @Test
  void getBatches_badRequest_whenMaxResultsLessThanOne() throws Exception {
    mockMvc.perform(get("/"+BATCHES_ENDPOINT_PATH).param("maxResults", "0"))
           .andExpect(status().isBadRequest());
  }

  @Test
  void getBatches_returnsOk_forEmptyAndNonEmpty() throws Exception {
    // first: empty list -> still OK
    when(reportingEngine.getLatestBatches(5)).thenReturn(Collections.emptyList());
    mockMvc.perform(get("/batches").param("maxResults", "5"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith("application/json"));

    BatchWithCounters batchMock = mock(BatchWithCounters.class);
    when(batchMock.getBatchId()).thenReturn(123L);
    when(batchMock.getLastUpdateTimeInMetisCore()).thenReturn(Instant.now());
    when(batchMock.getLastUpdateTimeInSolr()).thenReturn(Instant.now());
    when(batchMock.getCreationTime()).thenReturn(Instant.now());
    when(batchMock.getDatasetsProcessed()).thenReturn(42);
    reportingEngine.getLatestBatches(3);
    when(reportingEngine.getLatestBatches(3)).thenReturn(Collections.singletonList(batchMock));

    mockMvc.perform(get("/batches").param("maxResults", "3"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith("application/json"));
  }

  @Test
  void getHttpEntity_setsContentLength_andDispositionFilename() throws Exception {
    // exercise getHttpEntity via getReportByBatchId path to verify headers correlating to report bytes
    long batchId = 555L;
    Report r = mock(Report.class);
    String content = "a,b,c\n1,2,3\n";
    when(reportingEngine.getReportByBatchId(batchId)).thenReturn(r);
    when(r.getReportString()).thenReturn(content);
    when(r.getBatchId()).thenReturn(batchId);

    byte[] expected = content.getBytes();

    var mvcResult = mockMvc.perform(get("/" + REPORT_BY_BATCH_ID_ENDPOINT_PATH)
                               .param(ReportingController.BATCH_ID_ENDPOINT_PARAMETER, String.valueOf(batchId)))
                           .andExpect(status().isOk())
                           .andReturn();

    String cd = mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
    // ensure content-disposition present and content length correct
    assertNotNull(cd);
    int length = mvcResult.getResponse().getContentAsByteArray().length;
    assertEquals(expected.length, length);
    assertArrayEquals(expected, mvcResult.getResponse().getContentAsByteArray());
  }
}
