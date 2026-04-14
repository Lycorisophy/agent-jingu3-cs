-- v0.7 Workspace Phase 3：工作空间元数据与沙箱执行历史（对齐 docs/workspace/workspace-design.md §6～§7）

CREATE TABLE workspace (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    root_path VARCHAR(1024) NOT NULL,
    name VARCHAR(128),
    quota_mb BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_workspace_user ON workspace (user_id);

CREATE TABLE workspace_execution (
    id VARCHAR(36) PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    language VARCHAR(32) NOT NULL,
    run_mode VARCHAR(16) NOT NULL,
    relative_path VARCHAR(768),
    code_hash VARCHAR(64),
    stdout_snippet LONGTEXT,
    stderr_snippet LONGTEXT,
    exit_code INT NOT NULL,
    duration_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    error_type VARCHAR(64),
    timed_out BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_workspace_execution_ws FOREIGN KEY (workspace_id) REFERENCES workspace (id) ON DELETE CASCADE
);

CREATE INDEX idx_workspace_exec_user_time ON workspace_execution (user_id, created_at);
