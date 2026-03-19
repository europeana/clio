package eu.europeana.clio.common.persistence.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.Report;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.ReportRow;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportDaoTest {

  @Test
  void constructor_initializesHibernateSessionUtils() {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    // When
    ReportDao reportDao = new ReportDao(sessionFactory);
    // Then/
    assertNotNull(reportDao);
  }

  @Test
  void saveReport_throwsWhenBatchNotFound() {
    // Given
    try (MockedConstruction<HibernateSessionUtils> mockConstruction = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInTransaction(any()))
                .thenAnswer(invocation -> {
                  Object action = invocation.getArgument(0);
                  Session session = mock(Session.class);
                  doReturn(null).when(session).find(BatchRow.class, 246L);
                  return ((HibernateSessionUtils.DatabaseAction<?>) action).perform(session);
                })
    )) {
      ReportDao reportDao = new ReportDao(mock(SessionFactory.class));
      // When
      PersistenceException exception = assertThrows(PersistenceException.class, () -> reportDao.saveReport("report", 246L));
      // Then
      assertNotNull(mockConstruction.constructed());
      assertEquals("Cannot create run: batch with ID 246 does not exist.", exception.getMessage());
    }
  }

  @Test
  void saveReport_persistsAndReturnsGeneratedId() throws Exception {
    // Given
    try (MockedConstruction<HibernateSessionUtils> mockConstruction = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInTransaction(any())).thenAnswer(invocation -> {
              Object action = invocation.getArgument(0);
              Session session = mock(Session.class);
              BatchRow batchRow = mock(BatchRow.class);
              doReturn(batchRow).when(session).find(BatchRow.class, 7L);

              doAnswer(invocationOnMock -> {
                Object argument = invocationOnMock.getArgument(0);
                if (argument instanceof ReportRow reportRow) {
                  try {
                    Field field = ReportRow.class.getDeclaredField("reportId");
                    field.setAccessible(true);
                    field.setLong(reportRow, 321L);
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                }
                return null;
              }).when(session).persist(any());
              doNothing().when(session).flush();

              return ((HibernateSessionUtils.DatabaseAction<?>) action).perform(session);
            }))) {
      ReportDao reportDao = new ReportDao(mock(SessionFactory.class));
      // When
      long id = reportDao.saveReport("reportPayload", 7L);

      // Then
      assertNotNull(mockConstruction.constructed());
      assertEquals(321L, id);
    }
  }


  @Test
  void getLatestReports_returnsMappedReports() throws Exception {
    // Given
    try (MockedConstruction<HibernateSessionUtils> mockConstruction = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInSession(any())).thenAnswer(invocation -> {
              Object action = invocation.getArgument(0);
              Session session = mock(Session.class);

              Query namedQuery = mock(Query.class);
              doReturn(namedQuery).when(session).createNamedQuery(ReportRow.GET_LATEST_REPORT_QUERY, ReportRow.class);
              doReturn(namedQuery).when(namedQuery).setMaxResults(5);

              ReportRow reportRow = mock(ReportRow.class);
              BatchRow batchRow = mock(BatchRow.class);
              when(reportRow.getReportId()).thenReturn(11L);
              when(reportRow.getCreationTime()).thenReturn(Instant.ofEpochMilli(555L).toEpochMilli());
              when(reportRow.getReport()).thenReturn("reportData");
              when(reportRow.getBatch()).thenReturn(batchRow);
              when(batchRow.getBatchId()).thenReturn(22L);

              when(namedQuery.getResultList()).thenReturn(List.of(reportRow));

              return ((HibernateSessionUtils.DatabaseAction<?>) action).perform(session);
            }))) {

      ReportDao reportDao = new ReportDao(mock(SessionFactory.class));

      // When
      List<Report> reports = reportDao.getLatestReports(5);

      // Then
      assertNotNull(mockConstruction.constructed());
      assertEquals(1, reports.size());
      Report report = reports.getFirst();
      assertEquals(11L, report.getReportId());
      assertEquals(22L, report.getBatchId());
      assertEquals("reportData", report.getReportString());
    }
  }


  @Test
  void getAllReportDetails_returnsMappedReports() throws Exception {
    // Given
    try (MockedConstruction<HibernateSessionUtils> mockConstruction = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInSession(any())).thenAnswer(invocation -> {
              Object action = invocation.getArgument(0);
              Session session = mock(Session.class);

              Query namedQuery = mock(Query.class);
              doReturn(namedQuery).when(session).createNamedQuery(ReportRow.GET_ALL_REPORT_DETAILS_QUERY, ReportRow.class);

              ReportRow reportRow = mock(ReportRow.class);
              BatchRow batchRow = mock(BatchRow.class);
              when(reportRow.getReportId()).thenReturn(12L);
              when(reportRow.getCreationTime()).thenReturn(Instant.ofEpochMilli(666L).toEpochMilli());
              when(reportRow.getReport()).thenReturn("reportData");
              when(reportRow.getBatch()).thenReturn(batchRow);
              when(batchRow.getBatchId()).thenReturn(33L);

              when(namedQuery.getResultList()).thenReturn(List.of(reportRow));

              return ((HibernateSessionUtils.DatabaseAction<?>) action).perform(session);
            }))) {

      ReportDao dao = new ReportDao(mock(SessionFactory.class));
      // When
      List<Report> results = dao.getAllReportDetails();

      // Then
      assertNotNull(mockConstruction.constructed());
      assertEquals(1, results.size());

      Report report = results.getFirst();
      assertEquals(12L, report.getReportId());
      assertEquals(33L, report.getBatchId());
      assertEquals("reportData", report.getReportString());
    }
  }

  @Test
  void getReportByBatchId() throws Exception {
    // Given
    try (MockedConstruction<HibernateSessionUtils> mockConstruction = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInSession(any())).thenAnswer(invocation -> {
              Object action = invocation.getArgument(0);
              Session session = mock(Session.class);

              Query namedQuery = mock(Query.class);
              doReturn(namedQuery).when(session).createNamedQuery(ReportRow.GET_REPORT_BY_BATCH_ID_QUERY, ReportRow.class);
              doReturn(namedQuery).when(namedQuery).setParameter(ReportRow.BATCH_ID_PARAMETER, 7L);

              ReportRow reportRow = mock(ReportRow.class);
              BatchRow batchRow = mock(BatchRow.class);
              when(reportRow.getReportId()).thenReturn(77L);
              when(reportRow.getReport()).thenReturn("reportBatch");
              when(reportRow.getBatch()).thenReturn(batchRow);
              when(batchRow.getBatchId()).thenReturn(7L);
              when(reportRow.getCreationTime()).thenReturn(111L);

              doReturn(List.of(reportRow)).when(namedQuery).getResultList();

              return ((HibernateSessionUtils.DatabaseAction<?>) action).perform(session);
            }))) {

      ReportDao dao = new ReportDao(mock(SessionFactory.class));
      // When
      Report report = dao.getReportByBatchId(7L);

      // Then
      assertNotNull(mockConstruction.constructed());
      assertNotNull(report);
      assertEquals(77L, report.getReportId());
      assertEquals( "reportBatch", report.getReportString());
      assertEquals(111L, report.getCreationTime());
      assertEquals(7L, report.getBatchId());
    }
  }

  @Test
  void getReportByReportId() throws Exception {
    // Given
    try (MockedConstruction<HibernateSessionUtils> mockConstruction = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInSession(any())).thenAnswer(invocation -> {
              Object action = invocation.getArgument(0);
              Session session = mock(Session.class);
              Query namedQuery = mock(Query.class);
              doReturn(namedQuery).when(session).createNamedQuery(ReportRow.GET_REPORT_BY_REPORT_ID_QUERY, ReportRow.class);
              doReturn(namedQuery).when(namedQuery).setParameter(ReportRow.REPORT_ID_PARAMETER, 99L);
              ReportRow reportRow = mock(ReportRow.class);
              BatchRow batchRow = mock(BatchRow.class);
              when(reportRow.getReportId()).thenReturn(99L);
              when(reportRow.getReport()).thenReturn("reportData");
              when(reportRow.getBatch()).thenReturn(batchRow);
              when(batchRow.getBatchId()).thenReturn(8L);
              when(reportRow.getCreationTime()).thenReturn(222L);
              doReturn(List.of(reportRow)).when(namedQuery).getResultList();

              return ((HibernateSessionUtils.DatabaseAction<?>) action).perform(session);
            }))) {
      ReportDao dao = new ReportDao(mock(SessionFactory.class));

      // When
      Report report = dao.getReportByReportId(99L);

      // Then
      assertNotNull(mockConstruction.constructed());
      assertNotNull(report);
      assertEquals(99L, report.getReportId());
      assertEquals( "reportData", report.getReportString());
      assertEquals(222L, report.getCreationTime());
      assertEquals(8L, report.getBatchId());
    }
  }

  @Test
  void getReportEmptyByBatchId() throws PersistenceException {
    // given
    try (MockedConstruction<HibernateSessionUtils> mockConstruction = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInSession(any())).thenAnswer(invocation -> {
              Object action = invocation.getArgument(0);
              Session session = mock(Session.class);

              Query namedQuery = mock(Query.class);
              doReturn(namedQuery).when(session).createNamedQuery(ReportRow.GET_REPORT_BY_BATCH_ID_QUERY, ReportRow.class);
              doReturn(namedQuery).when(namedQuery).setParameter(ReportRow.BATCH_ID_PARAMETER, 5L);
              doReturn(List.of()).when(namedQuery).getResultList();

              return ((HibernateSessionUtils.DatabaseAction<?>) action).perform(session);
            }))) {

      ReportDao reportDao = new ReportDao(mock(SessionFactory.class));
      // When / Then
      assertNotNull(mockConstruction.constructed());
      assertNull(reportDao.getReportByBatchId(5L));
    }
  }

  @Test
  void getReportEmptyByReportId() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> mockConstruction = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInSession(any())).thenAnswer(invocation -> {
              Object action = invocation.getArgument(0);
              Session session = mock(Session.class);

              Query namedQuery = mock(Query.class);
              doReturn(namedQuery).when(session).createNamedQuery(ReportRow.GET_REPORT_BY_REPORT_ID_QUERY, ReportRow.class);
              doReturn(namedQuery).when(namedQuery).setParameter(ReportRow.REPORT_ID_PARAMETER, 2L);
              doReturn(List.of()).when(namedQuery).getResultList();

              return ((HibernateSessionUtils.DatabaseAction<?>) action).perform(session);
            }))) {
      ReportDao reportDao = new ReportDao(mock(SessionFactory.class));
      // When / Then
      assertNotNull(mockConstruction.constructed());
      assertNull(reportDao.getReportByReportId(2L));
    }
  }
}
