-- v0.6 M1：记忆域元数据表（对齐 里程碑切片 M1；详细字段随详细设计演进）
CREATE TABLE memory_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    summary VARCHAR(512),
    body LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_memory_user_created ON memory_entry (user_id, created_at);

CREATE TABLE fact_metadata (
    memory_entry_id BIGINT NOT NULL PRIMARY KEY,
    tag VARCHAR(128),
    CONSTRAINT fk_fact_metadata_entry FOREIGN KEY (memory_entry_id) REFERENCES memory_entry (id) ON DELETE CASCADE
);
