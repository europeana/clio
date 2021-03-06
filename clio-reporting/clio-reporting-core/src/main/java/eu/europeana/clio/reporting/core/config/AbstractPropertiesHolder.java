package eu.europeana.clio.reporting.core.config;

import eu.europeana.clio.common.persistence.ClioPersistenceConnectionProvider;

/**
 * Contains the properties required for the execution of the link checking functionality.
 */
public abstract class AbstractPropertiesHolder {

  // truststore
  protected static final String TRUSTSTORE_PATH_PROPERTY = "truststore.path";
  protected static final String TRUSTSTORE_PASS_PROPERTY = "truststore.password";

  // PostGreSQL
  protected static final String POSTGRES_SERVER_PROPERTY = "postgresql.server";
  protected static final String POSTGRES_USERNAME_PROPERTY = "postgresql.username";
  protected static final String POSTGRES_PASS_PROPERTY = "postgresql.password";

  // Reporting
  protected static final String REPORT_DATASET_LINK_TEMPLATE_PROPERTY = "report.dataset.link.template";

  protected abstract String getPostgresServer();

  protected abstract String getPostgresUsername();

  protected abstract String getPostgresPassword();

  public abstract String getTruststorePath();

  public abstract String getTruststorePassword();

  public abstract String getReportDatasetLinkTemplate();

  /**
   * Create a connected persistence connection.
   *
   * @return The connection.
   */
  public ClioPersistenceConnectionProvider getPersistenceConnectionProvider() {
    return new ClioPersistenceConnectionProvider(getPostgresServer(), getPostgresUsername(),
        getPostgresPassword());
  }
}
