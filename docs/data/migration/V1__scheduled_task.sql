-- Cron MVP：定时任务定义（H2 MySQL 模式 / MySQL 兼容）
CREATE TABLE scheduled_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id VARCHAR(64) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    conversation_id VARCHAR(128),
    cron_expression VARCHAR(256),
    next_run_at TIMESTAMP NOT NULL,
    payload_json LONGTEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMP,
    last_status VARCHAR(64),
    last_error LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scheduled_task_due ON scheduled_task (enabled, next_run_at);
CREATE INDEX idx_scheduled_task_owner ON scheduled_task (owner_user_id);
