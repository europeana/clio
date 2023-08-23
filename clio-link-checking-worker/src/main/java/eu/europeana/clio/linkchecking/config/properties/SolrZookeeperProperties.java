package eu.europeana.clio.linkchecking.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SolrZookeeperProperties {

    @Value("${solr.publish.hosts}")
    private String[] publishSolrHosts;
    @Value("${zookeeper.publish.hosts}")
    private String[] publishZookeeperHosts;
    @Value("${zookeeper.publish.port}")
    private int[] publishZookeeperPorts;
    @Value("${zookeeper.publish.chroot}")
    private String publishZookeeperChroot;
    @Value("${zookeeper.publish.defaultCollection}")
    private String publishZookeeperDefaultCollection;

    public String[] getPublishSolrHosts() {
        return publishSolrHosts;
    }

    public String[] getPublishZookeeperHosts() {
        return publishZookeeperHosts;
    }

    public int[] getPublishZookeeperPorts() {
        return publishZookeeperPorts;
    }

    public String getPublishZookeeperChroot() {
        return publishZookeeperChroot;
    }

    public String getPublishZookeeperDefaultCollection() {
        return publishZookeeperDefaultCollection;
    }
}
