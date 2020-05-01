
CREATE TABLE dataset (
    dataset_id VARCHAR(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    size INTEGER,
    last_index_time BIGINT,
    provider VARCHAR(64),
    data_provider VARCHAR(64),
    PRIMARY KEY (dataset_id)
);

CREATE TABLE batch (
    batch_id  BIGSERIAL NOT NULL,
    creation_time BIGINT NOT NULL,
    last_update_time_solr BIGINT NOT NULL,
    last_update_time_metis_core BIGINT NOT NULL,
    datasets_excluded_already_running INTEGER,
    datasets_excluded_not_indexed INTEGER,
    datasets_excluded_without_links INTEGER,
    PRIMARY KEY (batch_id)
);

CREATE TABLE run (
    run_id  BIGSERIAL NOT NULL,
    starting_time BIGINT NOT NULL,
    dataset_id VARCHAR(16) NOT NULL,
    batch_id BIGINT NOT NULL,
    PRIMARY KEY (run_id),
--    UNIQUE (dataset_id, batch_id),
    FOREIGN KEY (dataset_id) REFERENCES dataset,
    FOREIGN KEY (batch_id) REFERENCES batch
);

CREATE TABLE link (
    link_id  BIGSERIAL NOT NULL,
    run_id BIGINT NOT NULL,
    record_id VARCHAR(256) NOT NULL,
    record_edm_type VARCHAR(5),
    record_content_tier VARCHAR(1),
    record_metadata_tier VARCHAR(1),
    record_last_index_time BIGINT NOT NULL,
    link_type VARCHAR(11) NOT NULL,
    link_url VARCHAR(768) NOT NULL,
    server VARCHAR(128),
    error VARCHAR(512),
    checking_time BIGINT,
    PRIMARY KEY (link_id),
    FOREIGN KEY (run_id) REFERENCES run
);
CREATE INDEX ON link (server);
CREATE INDEX ON link (link_url);
