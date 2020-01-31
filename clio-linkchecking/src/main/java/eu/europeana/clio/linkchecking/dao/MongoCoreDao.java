package eu.europeana.clio.linkchecking.dao;

import com.mongodb.MongoClient;
import eu.europeana.clio.common.exception.ClioException;
import eu.europeana.clio.common.model.Dataset;
import eu.europeana.metis.core.dao.DatasetDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao;
import eu.europeana.metis.core.dao.WorkflowExecutionDao.PluginWithExecutionId;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProvider;
import eu.europeana.metis.core.mongo.MorphiaDatastoreProviderImpl;
import eu.europeana.metis.core.workflow.plugins.DataStatus;
import eu.europeana.metis.core.workflow.plugins.ExecutablePlugin;
import eu.europeana.metis.core.workflow.plugins.ExecutablePluginType;
import eu.europeana.metis.core.workflow.plugins.ExecutionProgress;
import eu.europeana.metis.core.workflow.plugins.MetisPlugin;
import eu.europeana.metis.core.workflow.plugins.PluginType;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

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
   * @return The dataset. Is not null.
   * @throws ClioException In case of problems with obtaining the dataset (including if a dataset
   * with the given ID doesn't exist or has not been published recently).
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
      throw new ClioException(
              "Cannot process dataset " + datasetId + ": it is not currently published.");
    }

    // Find out how many records the dataset has.
    final PluginWithExecutionId<ExecutablePlugin> latestSuccessfulExecutableIndex = workflowExecutionDao
            .getLatestSuccessfulExecutablePlugin(datasetId, Set.of(ExecutablePluginType.PUBLISH),
                    true);
    final int datasetSize = Optional.ofNullable(latestSuccessfulExecutableIndex)
            .map(PluginWithExecutionId::getPlugin).map(ExecutablePlugin::getExecutionProgress).map(
                    ExecutionProgress::getProcessedRecords).orElse(-1);

    // Convert to the dataset object we're interested in.
    final Instant lastIndexTime = Optional.ofNullable(latestSuccessfulExecutableIndex)
            .map(PluginWithExecutionId::getPlugin).map(ExecutablePlugin::getFinishedDate)
            .map(Date::toInstant).orElse(null);
    return new Dataset(metisDataset.getDatasetId(), metisDataset.getDatasetName(), datasetSize,
            lastIndexTime, metisDataset.getProvider(), metisDataset.getDataProvider());
  }
}
