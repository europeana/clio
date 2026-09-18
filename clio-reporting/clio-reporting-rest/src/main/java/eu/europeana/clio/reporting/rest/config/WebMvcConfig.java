package eu.europeana.clio.reporting.rest.config;

import eu.europeana.clio.common.config.properties.ClioConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final ClioConfigurationProperties clioConfigurationProperties;

  /**
   * Constructor.
   *
   * @param clioConfigurationProperties The properties.
   */
  @Autowired
  public WebMvcConfig(ClioConfigurationProperties clioConfigurationProperties) {
    this.clioConfigurationProperties = clioConfigurationProperties;
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addRedirectViewController("/", "/swagger-ui/index.html");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**").allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedOrigins(clioConfigurationProperties.allowedCorsHosts().toArray(String[]::new));
  }
}
