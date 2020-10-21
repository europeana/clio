package eu.europeana.clio.linkchecking.dao;

import static eu.europeana.metis.core.common.DaoFieldNames.DATASET_ID;
import static eu.europeana.metis.core.common.DaoFieldNames.ID;

import com.mongodb.client.MongoClient;
import dev.morphia.aggregation.experimental.Aggregation;
import dev.morphia.aggregation.experimental.expressions.Expressions;
import dev.morphia.aggregation.experimental.stages.Projection;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.metis.core.common.DaoFieldNames;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.PluginWithExecutionId;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao.ResultList;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProviderImpl;
import eu.europeana.metis.core.workflow.WorkflowExecution;
import eu.europeana.metis.core.workflow.WorkflowStatus;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.utils.ExternalRequestUtil;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Data access object for the Metis core Mongo.
 */
public class MongoCoreDao {

  private final MorphiaDatastoreProvider datastoreProvider;

  /**
   * Constructor.
   *
   * @param mongoClient The mongo client.
   * @param mongoDatabaseName The name of the database in the Mongo.
   */
  public MongoCoreDao(MongoClient mongoClient, String mongoDatabaseName) {
    this.datastoreProvider = new MorphiaDatastoreProviderImpl(mongoClient, mongoDatabaseName);
  }

  /**
   * Get a published dataset the Metis core persistence and translate it to a Clio dataset.
   *
   * @param datasetId The (Metis) dataset ID of the dataset.
   * @return The dataset. Is null if the dataset exists but is not processed.
   * @throws ClioException In case of problems with obtaining the dataset (including if a dataset
   * with the given ID doesn't exist).
   */
  public Dataset getPublishedDatasetById(String datasetId) throws ClioException {

    // Find the dataset from Metis.
    final eu.europeana.metis.core.dataset.Dataset metisDataset = new DatasetDao(datastoreProvider,
            null).getDatasetByDatasetId(datasetId);
    if (metisDataset == null) {
      throw new ClioException("Cannot process dataset " + datasetId + ": it does not exist.");
    }

    // Error checking
    final WorkflowExecutionDao workflowExecutionDao = new WorkflowExecutionDao(datastoreProvider);
    final MetisPlugin<?> latestSuccessfulIndex = Optional
            .ofNullable(workflowExecutionDao.getLatestSuccessfulPlugin(metisDataset.getDatasetId(),
                    Set.of(PluginType.PUBLISH, PluginType.REINDEX_TO_PUBLISH)))
            .map(PluginWithExecutionId::getPlugin)
            .orElse(null);
    final boolean noLatestIndexing = latestSuccessfulIndex == null;
    // TODO JV in the next Metis libraries version we can use MetisPlugin.getDataStatus().
    final boolean invalidLatestIndexing = latestSuccessfulIndex != null &&
            Optional.ofNullable(latestSuccessfulIndex.getDataStatus()).orElse(DataStatus.VALID)
                    != DataStatus.VALID;
    if (noLatestIndexing || invalidLatestIndexing) {
      return null;
    }

    // Find out how many records the dataset has.
    final PluginWithExecutionId<ExecutablePlugin> latestSuccessfulExecutableIndex = workflowExecutionDao
            .getLatestSuccessfulExecutablePlugin(datasetId, Set.of(ExecutablePluginType.PUBLISH),
                    false);
    final int datasetSize = Optional.ofNullable(latestSuccessfulExecutableIndex)
            .map(PluginWithExecutionId::getPlugin).map(ExecutablePlugin::getExecutionProgress)
            .map(progress -> progress.getProcessedRecords() - progress.getErrors()).orElse(-1);

    // Convert to the dataset object we're interested in.
    final Instant lastIndexTime = Optional.ofNullable(latestSuccessfulExecutableIndex)
            .map(PluginWithExecutionId::getPlugin).map(ExecutablePlugin::getFinishedDate)
            .map(Date::toInstant).orElse(null);
    return new Dataset(metisDataset.getDatasetId(), metisDataset.getDatasetName(), datasetSize,
            lastIndexTime, metisDataset.getProvider(), metisDataset.getDataProvider());
  }

  /**
   * Get the IDs of all datasets in one set.
   *
   * @return The set of IDs.
   */
  public Stream<String> getAllDatasetIds() {

    return ExternalRequestUtil.retryableExternalRequestForNetworkExceptions(() -> {

      // Create aggregation pipeline finding all datasets.
      final Aggregation<eu.europeana.metis.core.dataset.Dataset> pipeline = datastoreProvider
              .getDatastore().aggregate(eu.europeana.metis.core.dataset.Dataset.class);

      // The field name should be the field name in DatasetIdWrapper.
      final String datasetIdField = "datasetId";

      // Project the dataset ID to the right field name.
      pipeline.project(Projection.of().exclude(ID.getFieldName())
              .include(datasetIdField, Expressions.value(DATASET_ID.getFieldName())));

      // Perform the aggregation and add the IDs in the result set.
      final Iterator<DatasetIdWrapper> resultIterator = pipeline.execute(DatasetIdWrapper.class);
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(resultIterator, 0), false)
              .map(DatasetIdWrapper::getDatasetId);
    });
  }

  /**
   * Determines the last time that there was an update (i.e. a record was indexed). We do this by
   * returning the maximum of all update timestamps.
   *
   * @return The last update instant.
   */
  public Instant getLastUpdateTime() {
    final ResultList<WorkflowExecution> executions = new WorkflowExecutionDao(datastoreProvider)
            .getAllWorkflowExecutions(null, EnumSet.of(WorkflowStatus.FINISHED),
                    DaoFieldNames.FINISHED_DATE, false, 0, false);
    return executions.getResults().stream().findFirst().map(WorkflowExecution::getFinishedDate)
            .map(Date::toInstant).orElse(Instant.EPOCH);
  }

  /**
   * A wrapper class for a dataset ID that is used in an aggregation query.
   */
  private static class DatasetIdWrapper {

    // Name depends on the mongo aggregations in which it is used.
    private String datasetId;

    String getDatasetId() {
      return datasetId;
    }
  }
}
