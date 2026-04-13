-- v0.6：已写入 Milvus 的记忆条目标记（主键与 memory_entry 一致，便于删除与对账）
CREATE TABLE memory_embedding (
    memory_entry_id BIGINT NOT NULL PRIMARY KEY,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_memory_embedding_entry FOREIGN KEY (memory_entry_id) REFERENCES memory_entry (id) ON DELETE CASCADE
);
