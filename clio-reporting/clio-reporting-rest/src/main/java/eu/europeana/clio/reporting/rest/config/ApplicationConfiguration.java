package eu.europeana.clio.reporting.rest.config;

import eu.europeana.clio.reporting.core.ReportingEngine;
import eu.europeana.metis.utils.CustomTruststoreAppender;
import eu.europeana.metis.utils.apm.ElasticAPMConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Entry class with configuration fields and beans initialization for the application.
 */
@Configuration
@Import({ElasticAPMConfiguration.class})
@ComponentScan(basePackages = {"eu.europeana.clio.reporting.rest.controller"})
public class ApplicationConfiguration implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);
    private final ConfigurationPropertiesHolder propertiesHolder;

    public ApplicationConfiguration(ConfigurationPropertiesHolder propertiesHolder) throws CustomTruststoreAppender.TrustStoreConfigurationException {
        this.propertiesHolder = propertiesHolder;
        LOGGER.info("Found database connection: {}", propertiesHolder.getPostgresServer());
        ApplicationInitUtils.initializeApplication(propertiesHolder);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .resourceChain(false);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui/index.html");
    }

    @Bean
    ReportingEngine getReportingEngine() {
        return new ReportingEngine(propertiesHolder);
    }
}
