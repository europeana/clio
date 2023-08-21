package eu.europeana.clio.linkchecking.model;

import eu.europeana.clio.common.model.LinkType;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a sample record for a dataset.
 */
public class SampleRecord {

    private final String recordId;
    private final Instant lastIndexTime;
    private final String edmType;
    private final String contentTier;
    private final String metadataTier;
    private final Map<LinkType, Set<String>> links;

    /**
     * Constructor.
     *
     * @param recordId      The Europeana record ID.
     * @param lastIndexTime The last time this record was indexed.
     * @param edmType       The edm:type of the record.
     * @param contentTier   The content tier of the record.
     * @param metadataTier  The metadata tier of the record.
     * @param links         The links in the record, sorted by link type.
     */
    public SampleRecord(String recordId, Instant lastIndexTime, String edmType, String contentTier,
                        String metadataTier, Map<LinkType, Set<String>> links) {
        this.recordId = recordId;
        this.lastIndexTime = lastIndexTime;
        this.edmType = edmType;
        this.contentTier = contentTier;
        this.metadataTier = metadataTier;
        this.links = new EnumMap<>(links);
    }

    public String getRecordId() {
        return recordId;
    }

    public Instant getLastIndexTime() {
        return lastIndexTime;
    }

    public String getEdmType() {
        return edmType;
    }

    public String getContentTier() {
        return contentTier;
    }

    public String getMetadataTier() {
        return metadataTier;
    }

    public Map<LinkType, Set<String>> getLinks() {
        return Collections.unmodifiableMap(links);
    }
}
