package eu.europeana.clio.common.persistence.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import eu.europeana.clio.common.model.Dataset;
import eu.europeana.clio.common.model.Run;
import eu.europeana.clio.common.persistence.model.DatasetRow;
import eu.europeana.clio.common.persistence.model.RunRow;
import java.time.Instant;
import org.hibernate.SessionFactory;
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
