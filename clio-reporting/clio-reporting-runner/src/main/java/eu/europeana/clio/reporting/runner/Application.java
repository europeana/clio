package eu.europeana.clio.reporting.runner;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The Spring boot application entry point.
 */
@SpringBootApplication
public class Application {

    /**
     * The main spring boot method.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext run = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .run(args);
        run.close();
    }
}
