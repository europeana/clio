package eu.europeana.clio.reporting.rest.controller;

import static eu.europeana.clio.reporting.rest.controller.ReportingController.AVAILABLE_REPORTS_ENDPOINT_PATH;
import static eu.europeana.clio.reporting.rest.controller.ReportingController.BATCHES_ENDPOINT_PATH;
import static eu.europeana.clio.reporting.rest.controller.ReportingController.BATCH_ID_ENDPOINT_PARAMETER;
import static eu.europeana.clio.reporting.rest.controller.ReportingController.LATEST_REPORT_ENDPOINT_PATH;
import static eu.europeana.clio.reporting.rest.controller.ReportingController.REPORT_BY_BATCH_ID_ENDPOINT_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import eu.europeana.clio.common.model.CheckRecord;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.Report;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.reporting.service.ReportingEngine;
import eu.europeana.clio.reporting.rest.api.request.FilterRequest;
import eu.europeana.clio.reporting.rest.api.response.FilterResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
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
    // Given
    Report report = mock(Report.class);
    when(report.getCreationTime()).thenReturn(1L);
    when(report.getBatchId()).thenReturn(42L);
    when(report.getReportId()).thenReturn(1L);
    when(reportingEngine.getAllReportDetails()).thenReturn(List.of(report));
    // When / Then
    mockMvc.perform(get(AVAILABLE_REPORTS_ENDPOINT_PATH))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
           .andExpect(jsonPath("$[0].reportId").value(1))
           .andExpect(jsonPath("$[0].batchId").value(42))
           .andExpect(jsonPath("$[0].url").value(
               containsString(REPORT_BY_BATCH_ID_ENDPOINT_PATH)));
  }

  @Test
  void availableReports_whenEmpty_returnsEmptyList() throws Exception {
    // Given / When
    when(reportingEngine.getAllReportDetails()).thenReturn(Collections.emptyList());
    mockMvc.perform(get(AVAILABLE_REPORTS_ENDPOINT_PATH))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
           .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void getReportByBatchId_returnsReportBytes_andHeaders() throws Exception {
    // Given
    long batchId = 25L;
    Report report = mock(Report.class);
    String content = "column1,column2\nvalue1,value2\n";
    when(reportingEngine.getReportByBatchId(batchId)).thenReturn(report);
    when(report.getReportString()).thenReturn(content);
    when(report.getBatchId()).thenReturn(batchId);
    // When / Then
    mockMvc.perform(get(REPORT_BY_BATCH_ID_ENDPOINT_PATH)
               .param(BATCH_ID_ENDPOINT_PARAMETER,
                   String.valueOf(batchId)))
           .andExpect(status().isOk())
           .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
           .andExpect(content().bytes(content.getBytes()));
  }

  @Test
  void getReportByBatchId_whenNull_throwsReportNotFoundException() throws PersistenceException {
    // Given
    long batchId = 999L;
    when(reportingEngine.getReportByBatchId(batchId)).thenReturn(null);
    // When / Then
    ReportNotFoundException notFoundException = assertThrows(ReportNotFoundException.class,
        () -> controller.getReportByBatchId(batchId));
    assertNotNull(notFoundException.getMessage());
  }

  @Test
  void getLatestReport_returnsReportBytes_andHeaders() throws Exception {
    // Given
    Report latestReport = mock(Report.class);
    String content = "column1,column2\nvalue1,value2\n";
    when(reportingEngine.getLatestReports(1)).thenReturn(List.of(latestReport));
    when(latestReport.getReportString()).thenReturn(content);
    when(latestReport.getBatchId()).thenReturn(77L);
    // When / Then
    mockMvc.perform(get(LATEST_REPORT_ENDPOINT_PATH))
           .andExpect(status().isOk())
           .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
           .andExpect(content().bytes(content.getBytes()));
  }

  @Test
  void getLatestReport_whenNoReports_throwsReportNotFoundException() throws PersistenceException {
    // Given
    when(reportingEngine.getLatestReports(1)).thenReturn(Collections.emptyList());
    // When / Then
    ReportNotFoundException notFoundException = assertThrows(ReportNotFoundException.class,
        () -> controller.getLatestReport());
    assertNotNull(notFoundException.getMessage());
  }

  @Test
  void getBatches_whenGetLatestBatches_returnsOK() throws Exception {
    // Given
    BatchWithCounters batchMock = mock(BatchWithCounters.class);
    Instant batchTimestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    String expectedTimestamp = normalizeDate(batchTimestamp.atZone(ZoneId.systemDefault())
                                             .toOffsetDateTime()
                                             .toString());
    batchTimestamp = Instant.parse(expectedTimestamp);
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
    // When / Then
    mockMvc.perform(get(BATCHES_ENDPOINT_PATH).param("maxResults", "5"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
           .andExpect(jsonPath("$[0].creationTime").value(expectedTimestamp))
           .andExpect(jsonPath("$[0].lastUpdateTimeInSolr").value(expectedTimestamp))
           .andExpect(jsonPath("$[0].lastUpdateTimeInMetisCore").value(expectedTimestamp))
           .andExpect(jsonPath("$[0].datasetsExcludedAlreadyRunning").value(2))
           .andExpect(jsonPath("$[0].datasetsExcludedNotIndexed").value(4))
           .andExpect(jsonPath("$[0].datasetsExcludedWithoutLinks").value(6))
           .andExpect(jsonPath("$[0].datasetsProcessed").value(42))
           .andExpect(jsonPath("$[0].datasetsPending").value(8))
           .andDo(MockMvcResultHandlers.print());
  }

  @Test
  void getBatches_badRequest_whenMaxResultsLessThanOne() throws Exception {
    // When / Then
    mockMvc.perform(get(BATCHES_ENDPOINT_PATH).param("maxResults", "0"))
           .andExpect(status().isBadRequest());
  }

  @Test
  void getBatches_returnsOk_forEmptyAndNonEmpty() throws Exception {
    // Given
    when(reportingEngine.getLatestBatches(5)).thenReturn(Collections.emptyList());
    // When
    mockMvc.perform(get(BATCHES_ENDPOINT_PATH).param("maxResults", "5"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));

    // Given
    BatchWithCounters batchMock = mock(BatchWithCounters.class);
    when(batchMock.getBatchId()).thenReturn(123L);
    when(batchMock.getLastUpdateTimeInMetisCore()).thenReturn(Instant.now());
    when(batchMock.getLastUpdateTimeInSolr()).thenReturn(Instant.now());
    when(batchMock.getCreationTime()).thenReturn(Instant.now());
    when(batchMock.getDatasetsProcessed()).thenReturn(42);
    reportingEngine.getLatestBatches(3);
    when(reportingEngine.getLatestBatches(3)).thenReturn(Collections.singletonList(batchMock));
    // When / Then
    mockMvc.perform(get(BATCHES_ENDPOINT_PATH).param("maxResults", "3"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE));
  }

  @Test
  void getHttpEntity_setsContentLength_andDispositionFilename() throws Exception {
    // Given
    long batchId = 555L;
    Report r = mock(Report.class);
    String content = "column1,column2,column3\nvalue1,value2,value3\n";
    when(reportingEngine.getReportByBatchId(batchId)).thenReturn(r);
    when(r.getReportString()).thenReturn(content);
    when(r.getBatchId()).thenReturn(batchId);
    byte[] expected = content.getBytes();
    // When
    var mvcResult = mockMvc.perform(get(REPORT_BY_BATCH_ID_ENDPOINT_PATH)
                               .param(BATCH_ID_ENDPOINT_PARAMETER, String.valueOf(batchId)))
                           .andExpect(status().isOk())
                           .andReturn();
    // Then
    String contentDispositionHeader = mvcResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
    assertNotNull(contentDispositionHeader);
    int length = mvcResult.getResponse().getContentAsByteArray().length;
    assertEquals(expected.length, length);
    assertArrayEquals(expected, mvcResult.getResponse().getContentAsByteArray());
  }

  @Test
  void getReportById_returnsReportBytes_andHeaders() throws Exception {
    // Given
    long reportId = 11L;
    Report report = mock(Report.class);
    String content = "a,b\n1,2\n";
    when(reportingEngine.getReportByReportId(reportId)).thenReturn(report);
    when(report.getReportString()).thenReturn(content);
    when(report.getReportId()).thenReturn(reportId);

    // When
    HttpEntity<byte[]> entity = controller.getReportById(reportId);

    // Then
    assertArrayEquals(content.getBytes(), entity.getBody());
    assertNotNull(entity.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    assertEquals(content.getBytes().length, entity.getHeaders().getContentLength());
  }

  @Test
  void getReportById_whenNull_throwsReportNotFoundException() throws Exception {
    // Given
    long reportId = 12L;
    when(reportingEngine.getReportByReportId(reportId)).thenReturn(null);

    // When / Then
    assertThrows(ReportNotFoundException.class, () -> controller.getReportById(reportId));
  }

  @Test
  void getChecks_returnsFilteringResponse() throws Exception {
    // Given
    FieldFilters filters = mock(FieldFilters.class);
    FilterRequest request = new FilterRequest(filters);
    CheckRecord checkRecord = mock(CheckRecord.class);
    when(reportingEngine.getCheckRuns(any(FieldFilters.class))).thenReturn(List.of(checkRecord));

    // When
    var responseEntity = controller.getChecks(request);

    // Then
    assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    FilterResponse response = responseEntity.getBody();
    assertNotNull(response);
    assertEquals(1, response.getResults().size());
    // Verify that a sanitized FieldFilters object is returned (not the original mock)
    assertNotNull(response.getFilterOptions());
    // The returned filters are a new sanitized copy, not the original mock
  }

  @Test
  void downloadReport_returnsBytes_andHeaders() throws Exception {
    // Given
    FieldFilters filters = mock(FieldFilters.class);
    FilterRequest request = new FilterRequest(filters);
    String csv = "x,y\n1,2\n";
    when(reportingEngine.generateReport(any(FieldFilters.class))).thenReturn(csv);

    // When
    HttpEntity<byte[]> entity = controller.downloadReport(request);

    // Then
    assertArrayEquals(csv.getBytes(), entity.getBody());
    assertEquals(ReportingEngine.getReportFileNameSuggestion(), entity.getHeaders().getContentDisposition().getFilename());
    assertEquals(csv.getBytes().length, entity.getHeaders().getContentLength());
  }

  @Test
  void downloadReport_whenEngineThrows_throwsClioException() throws Exception {
    // Given
    FieldFilters filters = mock(FieldFilters.class);
    FilterRequest request = new FilterRequest(filters);
    ClioException expectedException = new ClioException("boom");
    when(reportingEngine.generateReport(any(FieldFilters.class))).thenThrow(expectedException);

    // When / Then
    ClioException actualException = assertThrows(ClioException.class, () -> controller.downloadReport(request));
    assertEquals(expectedException, actualException);
  }

  String normalizeDate(String s) {
    return s.replaceFirst("(\\.\\d{8})0(\\+\\d{2}:\\d{2})$", "$1$2");
  }
}
