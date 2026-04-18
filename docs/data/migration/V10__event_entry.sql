-- v0.x：事件与有向关系（MySQL 元数据 + Milvus 向量；事件侧不再使用 Elasticsearch）

CREATE TABLE event_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64),
    event_time VARCHAR(128),
    action LONGTEXT,
    result LONGTEXT,
    actors LONGTEXT,
    assertion VARCHAR(64),
    event_subject LONGTEXT,
    event_location LONGTEXT,
    trigger_terms LONGTEXT,
    modality VARCHAR(64),
    temporal_semantic VARCHAR(64),
    metadata LONGTEXT,
    message_id VARCHAR(128),
    vector_id VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_event_entry_user ON event_entry (user_id);
CREATE INDEX idx_event_entry_user_time ON event_entry (user_id, event_time);

CREATE TABLE event_relation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    event_a_id BIGINT NOT NULL,
    event_b_id BIGINT NOT NULL,
    rel_kind VARCHAR(32) NOT NULL,
    explanation LONGTEXT,
    confidence DOUBLE,
    source VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_event_rel_a FOREIGN KEY (event_a_id) REFERENCES event_entry (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_rel_b FOREIGN KEY (event_b_id) REFERENCES event_entry (id) ON DELETE CASCADE,
    CONSTRAINT uk_event_rel_triple UNIQUE (event_a_id, event_b_id, rel_kind)
);

CREATE INDEX idx_event_rel_user ON event_relation (user_id);
CREATE INDEX idx_event_rel_a ON event_relation (event_a_id);
CREATE INDEX idx_event_rel_b ON event_relation (event_b_id);
