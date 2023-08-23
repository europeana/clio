package eu.europeana.clio.common.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Class that is used to read all configuration properties for the application.
 * <p>
 * It uses {@link PropertySource} to identify the properties on application startup
 * </p>
 */
@Configuration
public class TruststoreProperties {

  @Value("${truststore.path}")
  private String truststorePath;
  @Value("${truststore.password}")
  private String truststorePassword;

  public String getTruststorePath() {
    return truststorePath;
  }

  public String getTruststorePassword() {
    return truststorePassword;
  }
}
