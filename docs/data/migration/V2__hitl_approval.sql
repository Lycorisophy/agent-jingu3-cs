-- HITL MVP：人在环审批队列（对齐 v0.5-横切能力极简设计 §3 B2）
CREATE TABLE hitl_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(128) NOT NULL,
    run_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolver_user_id VARCHAR(64)
);

CREATE INDEX idx_hitl_pending_conv ON hitl_approval (status, conversation_id);
CREATE INDEX idx_hitl_created ON hitl_approval (created_at);
