package eu.europeana.clio.common.persistence.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.exception.PersistenceException;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.model.Link;
import eu.europeana.clio.common.model.LinkType;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.HibernateSessionUtils;
import eu.europeana.clio.common.persistence.StreamResult;
import eu.europeana.clio.common.persistence.dao.LinkDao.RunWithLink;
import eu.europeana.clio.common.persistence.dao.LinkDao.UncheckedLinkData;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkDaoTest {

  @Test
  void constructor_initializesHibernateSessionUtils() {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);

    // When
    LinkDao dao = new LinkDao(sessionFactory);

    // Then
    assertNotNull(dao);
  }

  @Test
  void createUncheckedLink_IS_SHOWN_AT_succeeds() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) ->
            when(mock.performInTransaction(any())).thenAnswer(invocation -> {
              Session session = mock(Session.class);
              RunRow run = mock(RunRow.class);
              when(session.find(RunRow.class, 10L)).thenReturn(run);

              doAnswer(inv -> {
                LinkRow linkRow = inv.getArgument(0);
                try {
                  Field field = LinkRow.class.getDeclaredField("linkId");
                  field.setAccessible(true);
                  field.setLong(linkRow, 99L);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
                return null;
              }).when(session).persist(any());

              doNothing().when(session).flush();

              return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
            }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          10L, "recordId", Instant.ofEpochMilli(1000), "edmType", "contentTier", "metaTier",
          "http://example.com", LinkType.IS_SHOWN_AT
      );
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      long linkId = linkDao.createUncheckedLink(data);

      // Then
      assertEquals(99L, linkId);
    }
  }

  @Test
  void createUncheckedLink_IS_SHOWN_BY_succeeds() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);
          RunRow run = mock(RunRow.class);
          when(session.find(RunRow.class, 20L)).thenReturn(run);

          doAnswer(inv -> {
            LinkRow linkRow = inv.getArgument(0);
            try {
              Field field = LinkRow.class.getDeclaredField("linkId");
              field.setAccessible(true);
              field.setLong(linkRow, 88L);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return null;
          }).when(session).persist(any());

          doNothing().when(session).flush();

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          20L, "recordId", Instant.ofEpochMilli(2000), "edmType", "contentTier", "metaTier",
          "http://example.org", LinkType.IS_SHOWN_BY
      );
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      long id = linkDao.createUncheckedLink(data);

      // Then
      assertEquals(88L, id);
    }
  }

  @Test
  void createUncheckedLink_throwsWhenRunNotFound() {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);
          when(session.find(RunRow.class, 999L)).thenReturn(null);

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          999L, "recordId", Instant.now(), "type", "tier", "tier",
          "http://brokenlink.com", LinkType.IS_SHOWN_AT
      );
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      PersistenceException persistenceException = assertThrows(PersistenceException.class,
          () -> linkDao.createUncheckedLink(data));
      // Then
      assertEquals("Cannot create link: run with ID 999 does not exist.", persistenceException.getMessage());
    }
  }

  @Test
  void createUncheckedLink_computesServerCorrectly() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);
          RunRow run = mock(RunRow.class);
          when(session.find(RunRow.class, 5L)).thenReturn(run);

          doAnswer(inv -> {
            LinkRow linkRow = inv.getArgument(0);
            assertEquals("https://example.com/", linkRow.getServer());
            try {
              Field field = LinkRow.class.getDeclaredField("linkId");
              field.setAccessible(true);
              field.setLong(linkRow, 77L);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return null;
          }).when(session).persist(any());

          doNothing().when(session).flush();

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          5L, "recordId", Instant.now(), "type", "tier", "tier",
          "https://example.com/path/to/page", LinkType.IS_SHOWN_AT
      );
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      long id = linkDao.createUncheckedLink(data);

      // Then
      assertEquals(77L, id);
    }
  }

  @Test
  void createUncheckedLink_handlesInvalidUrl() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);
          RunRow run = mock(RunRow.class);
          when(session.find(RunRow.class, 15L)).thenReturn(run);

          doAnswer(inv -> {
            LinkRow linkRow = inv.getArgument(0);
            assertNull(linkRow.getServer());
            try {
              Field field = LinkRow.class.getDeclaredField("linkId");
              field.setAccessible(true);
              field.setLong(linkRow, 66L);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return null;
          }).when(session).persist(any());

          doNothing().when(session).flush();

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          15L, "rec", Instant.now(), "type", "tier", "tier",
          "not a valid url", LinkType.IS_SHOWN_AT
      );
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      long id = linkDao.createUncheckedLink(data);

      // Then
      assertEquals(66L, id);
    }
  }

  @Test
  void getAllUncheckedLinks_returnsStreamResult() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> {
          StreamResult<Link> streamResult = mock(StreamResult.class);
          doReturn(streamResult).when(mock).performForStream(any());
        })) {

      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      StreamResult<Link> result = linkDao.getAllUncheckedLinks();

      // Then
      assertNotNull(result);
    }
  }

  @Test
  void registerLinkChecking_updatesLinksWithError() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);

          Query<LinkRow> namedQuery = mock(Query.class);
          when(session.createNamedQuery(LinkRow.GET_UNCHECKED_LINKS_BY_URL, LinkRow.class)).thenReturn(namedQuery);
          when(namedQuery.setParameter(LinkRow.LINK_URL_PARAMETER, "http://url")).thenReturn(namedQuery);

          LinkRow linkRow = mock(LinkRow.class);
          when(namedQuery.getResultList()).thenReturn(List.of(linkRow));

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      linkDao.registerLinkChecking("http://url", "404");

      // Then
      assertNotNull(linkDao);
    }
  }

  @Test
  void registerLinkChecking_updatesMultipleLinksWithSameUrl() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);

          Query<LinkRow> namedQuery = mock(Query.class);
          when(session.createNamedQuery(LinkRow.GET_UNCHECKED_LINKS_BY_URL, LinkRow.class)).thenReturn(namedQuery);
          when(namedQuery.setParameter(LinkRow.LINK_URL_PARAMETER, "http://url")).thenReturn(namedQuery);

          LinkRow linkRow1 = mock(LinkRow.class);
          LinkRow linkRow2 = mock(LinkRow.class);
          when(namedQuery.getResultList()).thenReturn(List.of(linkRow1, linkRow2));

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      linkDao.registerLinkChecking("http://url", "500");

      // Then
      assertNotNull(linkDao);
    }
  }

  @Test
  void registerLinkChecking_updatesLinksWithNullError() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);

          Query<LinkRow> namedQuery = mock(Query.class);
          when(session.createNamedQuery(LinkRow.GET_UNCHECKED_LINKS_BY_URL, LinkRow.class)).thenReturn(namedQuery);
          when(namedQuery.setParameter(LinkRow.LINK_URL_PARAMETER, "http://valid")).thenReturn(namedQuery);

          LinkRow linkRow = mock(LinkRow.class);
          when(namedQuery.getResultList()).thenReturn(List.of(linkRow));

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      linkDao.registerLinkChecking("http://valid", null);

      // Then
      assertNotNull(linkDao);
    }
  }

  @Test
  void getBrokenLinksInLatestCompletedRuns_returnsStreamResult() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> {
          StreamResult<RunWithLink> streamResult = mock(StreamResult.class);
          doReturn(streamResult).when(mock).performForStream(any());
        })) {

      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      StreamResult<RunWithLink> result = linkDao.getBrokenLinksInLatestCompletedRuns();

      // Then
      assertNotNull(result);
    }
  }

  @Test
  void getLinksWithRunsForFilters_buildsFilteredQueryAndReturnsStreamResult() throws PersistenceException {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    Session session = mock(Session.class);
    HibernateCriteriaBuilder criteriaBuilder = mock(HibernateCriteriaBuilder.class, RETURNS_DEEP_STUBS);
    JpaCriteriaQuery<RunWithLink> criteriaQuery = mock(JpaCriteriaQuery.class, RETURNS_DEEP_STUBS);
    Query<RunWithLink> query = mock(Query.class);
    StreamResult<RunWithLink> streamResult = mock(StreamResult.class);
    RunWithLink runWithLink = new RunWithLink(mock(Run.class), mock(Link.class));
    Stream<RunWithLink> resultStream = Stream.of(runWithLink);

    when(session.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    when(criteriaBuilder.createQuery(RunWithLink.class)).thenReturn(criteriaQuery);
    when(session.createQuery(criteriaQuery)).thenReturn(query);
    when(query.getResultStream()).thenReturn(resultStream);

    FieldFilters filters = new FieldFilters();
    filters.setProvider(new TreeSet<>(Set.of("provider")));
    filters.setDataProvider(new TreeSet<>(Set.of("data-provider")));
    filters.setDatasetId(new TreeSet<>(Set.of("dataset-id")));
    filters.setDatasetName(new TreeSet<>(Set.of("dataset-name")));
    filters.setExcludedDatasetId(new TreeSet<>(Set.of("excluded-dataset")));
    filters.setDateFrom(LocalDate.of(2026, 1, 1));
    filters.setDateTo(LocalDate.of(2026, 1, 31));
    filters.setPercentLinksInOperationFrom(20);
    filters.setPercentLinksInOperationTo(80);


    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, context) -> when(mock.performForStream(any())).thenAnswer(invocation -> {
          HibernateSessionUtils.DatabaseAction<Stream<RunWithLink>> action = invocation.getArgument(0);
          action.perform(session);
          return streamResult;
        }))) {
      LinkDao linkDao = new LinkDao(sessionFactory);
      // When
      StreamResult<RunWithLink> result = linkDao.getLinksWithRunsForFilters(filters);

      // Then
      assertEquals(streamResult, result);
      verify(query, times(13)).setParameter(nullable(String.class), any());
      verify(query).setHint("org.hibernate.fetchSize", LinkDao.FETCH_SIZE);
      verify(query).setHint("org.hibernate.readOnly", true);
      verify(query).getResultStream();
      verify(criteriaBuilder).parameter(Set.class, FieldNames.DATASET_PROVIDER_DB + "Parameter");
      verify(criteriaBuilder).parameter(Set.class, FieldNames.DATASET_DATA_PROVIDER_DB + "Parameter");
      verify(criteriaBuilder).parameter(Set.class, FieldNames.DATASET_ID_DB + "Parameter");
      verify(criteriaBuilder).parameter(Set.class, FieldNames.DATASET_NAME_DB + "Parameter");
      verify(criteriaBuilder).parameter(Set.class, FieldNames.EXCLUDED_DATASET_ID);
      verify(criteriaBuilder, times(2)).parameter(Long.class, FieldNames.STARTING_WINDOW_TIME_DB);
      verify(criteriaBuilder, times(2)).parameter(Long.class, FieldNames.ENDING_WINDOW_TIME_DB);
      verify(criteriaBuilder).parameter(Long.class, FieldNames.STARTING_TIME_DB);
      verify(criteriaBuilder).parameter(Long.class, FieldNames.ENDING_TIME_DB);
      verify(criteriaBuilder).parameter(Integer.class, FieldNames.PERCENT_LINKS_IN_OPERATION_FROM_DB);
      verify(criteriaBuilder).parameter(Integer.class, FieldNames.PERCENT_LINKS_IN_OPERATION_TO_DB);
    }
  }

  @Test
  void uncheckedLinkDataRecord_createsInstanceCorrectly() {
    // Given
    Instant time = Instant.now();

    // When
    UncheckedLinkData data = new UncheckedLinkData(
        1L, "recordId", time, "edmType", "contentTier", "metaTier",
        "http://url", LinkType.IS_SHOWN_AT
    );

    // Then
    assertEquals(1L, data.runId());
    assertEquals("recordId", data.recordId());
    assertEquals(time, data.recordLastIndexTime());
    assertEquals("edmType", data.recordEdmType());
    assertEquals("contentTier", data.recordContentTier());
    assertEquals("metaTier", data.recordMetadataTier());
    assertEquals("http://url", data.linkUrl());
    assertEquals(LinkType.IS_SHOWN_AT, data.linkType());
  }

  @Test
  void runWithLinkRecord_createsInstanceCorrectly() {
    // Given
    Run run = mock(Run.class);
    Link link = mock(Link.class);

    // When
    RunWithLink runWithLink = new RunWithLink(run, link);

    // Then
    assertEquals(run, runWithLink.run());
    assertEquals(link, runWithLink.link());
  }

  @Test
  void runWithLinkRecord_createsInstanceFromRowsCorrectly() {
    // Given
    RunRow runRow = mock(RunRow.class);
    when(runRow.getRunId()).thenReturn(1L);
    when(runRow.getStartingTime()).thenReturn(Instant.now());

    DatasetRow datasetRow = mock(DatasetRow.class);
    when(datasetRow.getDatasetId()).thenReturn("testDatasetId");
    when(runRow.getDataset()).thenReturn(datasetRow);

    LinkRow linkRow = mock(LinkRow.class);
    when(linkRow.getLinkId()).thenReturn(100L);
    when(linkRow.getRecordId()).thenReturn("testRecord");
    when(linkRow.getRecordLastIndexTime()).thenReturn(Instant.ofEpochMilli(5000));
    when(linkRow.getRecordEdmType()).thenReturn("testType");
    when(linkRow.getRecordContentTier()).thenReturn("testContentTier");
    when(linkRow.getRecordMetadataTier()).thenReturn("testMetadataTier");
    when(linkRow.getLinkType()).thenReturn(LinkRow.LinkType.IS_SHOWN_AT);
    when(linkRow.getLinkUrl()).thenReturn("http://example.com");
    when(linkRow.getServer()).thenReturn("http://example.com/");
    when(linkRow.getError()).thenReturn(null);
    when(linkRow.getCheckingTime()).thenReturn(Instant.ofEpochMilli(6000));

    // When
    RunWithLink runWithLink = new RunWithLink(runRow, linkRow);

    // Then
    assertNotNull(runWithLink);
    assertNotNull(runWithLink.run());
    assertNotNull(runWithLink.link());
    assertEquals(1L, runWithLink.run().getRunId());
    assertEquals("http://example.com", runWithLink.link().getLinkUrl());
  }

  @Test
  void createUncheckedLink_computesServerWithSchemeButNoAuthority() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);
          RunRow run = mock(RunRow.class);
          when(session.find(RunRow.class, 25L)).thenReturn(run);

          doAnswer(inv -> {
            LinkRow linkRow = inv.getArgument(0);
            assertNull(linkRow.getServer());
            try {
              Field field = LinkRow.class.getDeclaredField("linkId");
              field.setAccessible(true);
              field.setLong(linkRow, 55L);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return null;
          }).when(session).persist(any());

          doNothing().when(session).flush();

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          25L, "rec", Instant.now(), "type", "tier", "tier",
          "://no-authority", eu.europeana.clio.common.model.LinkType.IS_SHOWN_AT
      );
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      long id = linkDao.createUncheckedLink(data);

      // Then
      assertEquals(55L, id);
    }
  }

  @Test
  void createUncheckedLink_computesServerWithNoScheme() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);
          RunRow run = mock(RunRow.class);
          when(session.find(RunRow.class, 35L)).thenReturn(run);

          doAnswer(inv -> {
            LinkRow linkRow = inv.getArgument(0);
            assertNull(linkRow.getServer());
            try {
              Field field = LinkRow.class.getDeclaredField("linkId");
              field.setAccessible(true);
              field.setLong(linkRow, 44L);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return null;
          }).when(session).persist(any());

          doNothing().when(session).flush();

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          35L, "rec", Instant.now(), "type", "tier", "tier",
          "example.com/path", LinkType.IS_SHOWN_BY
      );
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      long id = linkDao.createUncheckedLink(data);

      // Then
      assertEquals(44L, id);
    }
  }

  @Test
  void createUncheckedLink_withValidUrl_computesServerCorrectlyWithSchemeAndAuthority() throws PersistenceException {
    // Given
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);
          RunRow run = mock(RunRow.class);
          when(session.find(RunRow.class, 45L)).thenReturn(run);

          doAnswer(inv -> {
            LinkRow linkRow = inv.getArgument(0);
            assertEquals("http://example.org:8080/", linkRow.getServer());
            try {
              Field field = LinkRow.class.getDeclaredField("linkId");
              field.setAccessible(true);
              field.setLong(linkRow, 33L);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return null;
          }).when(session).persist(any());

          doNothing().when(session).flush();

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          45L, "recordId", Instant.now(), "type", "tier", "tier",
          "http://example.org:8080/path/to/resource", LinkType.IS_SHOWN_AT
      );
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      long id = linkDao.createUncheckedLink(data);

      // Then
      assertEquals(33L, id);
    }
  }

  @Test
  void computeServerPrivateMethod_handlesExceptionGracefully() throws PersistenceException {
    // Given - testing through createUncheckedLink with a URL that will trigger the catch block
    try (MockedConstruction<HibernateSessionUtils> ignored = mockConstruction(HibernateSessionUtils.class,
        (mock, ctx) -> when(mock.performInTransaction(any())).thenAnswer(invocation -> {
          Session session = mock(Session.class);
          RunRow run = mock(RunRow.class);
          when(session.find(RunRow.class, 50L)).thenReturn(run);

          doAnswer(inv -> {
            LinkRow linkRow = inv.getArgument(0);
            // The server should be null due to exception in computeServer
            assertNull(linkRow.getServer());
            try {
              Field field = LinkRow.class.getDeclaredField("linkId");
              field.setAccessible(true);
              field.setLong(linkRow, 22L);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return null;
          }).when(session).persist(any());

          doNothing().when(session).flush();

          return ((HibernateSessionUtils.DatabaseAction<?>) invocation.getArgument(0)).perform(session);
        }))) {

      UncheckedLinkData data = new UncheckedLinkData(
          50L, "rec", Instant.now(), "type", "tier", "tier",
          "ht\ttp://invalid", LinkType.IS_SHOWN_AT);
      LinkDao linkDao = new LinkDao(mock(SessionFactory.class));

      // When
      long id = linkDao.createUncheckedLink(data);

      // Then
      assertEquals(22L, id);
    }
  }
}
