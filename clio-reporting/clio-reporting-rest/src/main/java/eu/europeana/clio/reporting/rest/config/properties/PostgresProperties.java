package eu.europeana.clio.reporting.rest.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PostgresProperties {

    // PostgreSQL
    @Value("${postgresql.server}")
    private String postgresServer;
    @Value("${postgresql.username}")
    private String postgresUsername;
    @Value("${postgresql.password}")
    private String postgresPassword;

    public String getPostgresServer() {
        return postgresServer;
    }

    public String getPostgresUsername() {
        return postgresUsername;
    }

    public String getPostgresPassword() {
        return postgresPassword;
    }
}
