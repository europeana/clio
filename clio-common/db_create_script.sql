
CREATE TABLE dataset (
    dataset_id VARCHAR(16) NOT NULL,
    name VARCHAR(64) NOT NULL,
    size INTEGER NOT NULL,
    provider VARCHAR(64),
    data_provider VARCHAR(64),
    PRIMARY KEY (dataset_id)
);

CREATE TABLE run (
    run_id  BIGSERIAL NOT NULL,
    starting_time BIGINT NOT NULL,
    dataset_id VARCHAR(16) NOT NULL,
    PRIMARY KEY (run_id),
    UNIQUE (dataset_id, starting_time),
    FOREIGN KEY (dataset_id) REFERENCES dataset
);

CREATE TABLE link (
    link_id  BIGSERIAL NOT NULL,
    run_id BIGINT NOT NULL,
    record_id VARCHAR(256) NOT NULL,
    link_type VARCHAR(11) NOT NULL,
    link_url VARCHAR(256) NOT NULL,
    server VARCHAR(128),
    error VARCHAR(512),
    checking_time int8,
    PRIMARY KEY (link_id),
    UNIQUE (run_id, link_url, link_type),
    FOREIGN KEY (run_id) REFERENCES run
);
CREATE INDEX ON link (server);
CREATE INDEX ON link (link_url);
