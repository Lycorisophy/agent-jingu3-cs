-- DST 占位：对话侧栏状态（对齐 v0.5-横切能力极简设计 §4 B3）
CREATE TABLE dialogue_state (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(128) NOT NULL,
    schema_version VARCHAR(32) NOT NULL DEFAULT '1',
    state_json CLOB NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dialogue_state_conv (conversation_id)
);
