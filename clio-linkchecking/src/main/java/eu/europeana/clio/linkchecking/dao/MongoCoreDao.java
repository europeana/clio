package eu.europeana.clio.linkchecking.dao;

import static eu.europeana.metis.core.common.DaoFieldNames.DATASET_ID;
import static eu.europeana.metis.core.common.DaoFieldNames.ID;

import com.mongodb.MongoClient;
import dev.morphia.aggregation.AggregationPipeline;
import dev.morphia.aggregation.Projection;
import dev.morphia.query.Query;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.PluginWithExecutionId;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProviderImpl;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import eu.europeana.metis.utils.ExternalRequestUtil;
import java.time.Instant;
import java.util.Date;
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
    final MetisPlugin latestSuccessfulIndex = workflowExecutionDao
            .getLatestSuccessfulPlugin(metisDataset.getDatasetId(),
                    Set.of(PluginType.PUBLISH, PluginType.REINDEX_TO_PUBLISH));
    final boolean noLatestIndexing = latestSuccessfulIndex == null;
    final boolean invalidLatestIndexing = (latestSuccessfulIndex instanceof ExecutablePlugin)
            && ((ExecutablePlugin) latestSuccessfulIndex).getDataStatus() != DataStatus.VALID;
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

    return ExternalRequestUtil.retryableExternalRequestConnectionReset(() -> {

      // Create aggregation pipeline finding all datasets.
      final AggregationPipeline pipeline = datastoreProvider.getDatastore()
              .createAggregation(eu.europeana.metis.core.dataset.Dataset.class);
      final Query<eu.europeana.metis.core.dataset.Dataset> query = datastoreProvider.getDatastore()
              .createQuery(eu.europeana.metis.core.dataset.Dataset.class);
      pipeline.match(query);

      // The field name should be the field name in DatasetIdWrapper.
      final String datasetIdField = "datasetId";

      // Project the dataset ID to the right field name.
      pipeline.project(Projection.projection(ID.getFieldName()).suppress(),
              Projection.projection(datasetIdField, DATASET_ID.getFieldName()));

      // Perform the aggregation and add the IDs in the result set.
      final Iterator<DatasetIdWrapper> resultIterator = pipeline.aggregate(DatasetIdWrapper.class);
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(resultIterator, 0), false)
              .map(DatasetIdWrapper::getDatasetId);
    });
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
