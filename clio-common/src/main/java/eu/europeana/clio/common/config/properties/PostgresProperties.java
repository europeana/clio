package eu.europeana.clio.common.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostgresProperties {

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
