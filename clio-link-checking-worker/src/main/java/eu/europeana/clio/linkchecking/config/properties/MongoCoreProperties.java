package eu.europeana.clio.linkchecking.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoCoreProperties {

    //Mongo Metis Core
    @Value("${mongo.core.hosts}")
    private String[] mongoCoreHosts;
    @Value("${mongo.core.port}")
    private int[] mongoCorePorts;
    @Value("${mongo.core.username}")
    private String mongoCoreUsername;
    @Value("${mongo.core.password}")
    private String mongoCorePassword;
    @Value("${mongo.core.authentication.db}")
    private String mongoCoreAuthenticationDatabase;
    @Value("${mongo.core.db}")
    private String mongoCoreDatabase;
    @Value("${mongo.core.enable.ssl}")
    private boolean mongoCoreEnableSsl;
    @Value("${mongo.core.application.name}")
    private String mongoCoreApplicationName;

    public String[] getMongoCoreHosts() {
        return mongoCoreHosts;
    }

    public int[] getMongoCorePorts() {
        return mongoCorePorts;
    }

    public String getMongoCoreUsername() {
        return mongoCoreUsername;
    }

    public String getMongoCorePassword() {
        return mongoCorePassword;
    }

    public String getMongoCoreAuthenticationDatabase() {
        return mongoCoreAuthenticationDatabase;
    }

    public String getMongoCoreDatabase() {
        return mongoCoreDatabase;
    }

    public boolean isMongoCoreEnableSsl() {
        return mongoCoreEnableSsl;
    }

    public String getMongoCoreApplicationName() {
        return mongoCoreApplicationName;
    }
}
