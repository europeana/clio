package eu.europeana.clio.common.persistence.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.model.DatasetSummary;
import eu.europeana.clio.common.persistence.dao.DatasetDao.CommonDatasetQueryParts;
import eu.europeana.clio.common.persistence.model.BatchRow;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.LinkRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCoalesce;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSearchedCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunDaoTest {

  @Test
  void constructor_initializesHibernateSessionUtils() {
    // Given
    SessionFactory sessionFactory = mock(SessionFactory.class);
    // When
    RunDao runDao = new RunDao(sessionFactory);
    // Then
    assertNotNull(runDao);
  }

  @Test
  void convert_convertsRunRowToRun() {
    // Given
    RunRow runRow = mock(RunRow.class);
    long runId = 456L;
    Instant startingTime = Instant.now();
    DatasetRow datasetRow = mock(DatasetRow.class);

    when(runRow.getRunId()).thenReturn(runId);
    when(runRow.getStartingTime()).thenReturn(startingTime);
    when(runRow.getDataset()).thenReturn(datasetRow);

    try (MockedStatic<DatasetDao> datasetDaoMock = mockStatic(DatasetDao.class)) {
      var mockDataset = mock(Dataset.class);
      datasetDaoMock.when(() -> DatasetDao.convert(datasetRow)).thenReturn(mockDataset);

      // When
      Run result = RunDao.convert(runRow);

      // Then
      assertNotNull(result);
      assertEquals(runId, result.getRunId());
      assertEquals(startingTime, result.getStartingTime());
      assertEquals(mockDataset, result.getDataset());
    }
  }

  @Test
  void convert_preservesAllRunAttributes() {
    // Given
    RunRow runRow = mock(RunRow.class);
    long runId = 789L;
    Instant startingTime = Instant.ofEpochMilli(1234567890L);
    DatasetRow datasetRow = mock(DatasetRow.class);

    when(runRow.getRunId()).thenReturn(runId);
    when(runRow.getStartingTime()).thenReturn(startingTime);
    when(runRow.getDataset()).thenReturn(datasetRow);

    try (MockedStatic<DatasetDao> datasetDaoMock = mockStatic(DatasetDao.class)) {
      var mockDataset = mock(Dataset.class);
      datasetDaoMock.when(() -> DatasetDao.convert(datasetRow)).thenReturn(mockDataset);

      // When
      Run result = RunDao.convert(runRow);

      // Then
      assertEquals(789L, result.getRunId());
      assertEquals(Instant.ofEpochMilli(1234567890L), result.getStartingTime());
      assertEquals(mockDataset, result.getDataset());
    }
  }
}
