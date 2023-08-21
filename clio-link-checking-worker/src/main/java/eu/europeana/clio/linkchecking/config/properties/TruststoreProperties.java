package eu.europeana.clio.linkchecking.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * Class that is used to read all configuration properties for the application.
 * <p>
 * It uses {@link PropertySource} to identify the properties on application startup
 * </p>
 */
@Component
public class TruststoreProperties {

  //Custom truststore
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
