-- v0.x：用户、会话、消息、工具调用、技能元数据（与 docs/设计/系统数据存储-物化清单与测试数据.md 对齐）
-- 说明：users.id 使用 VARCHAR(64) 以兼容单用户种子 001；JSON 语义字段用 LONGTEXT 以兼容 H2 MySQL 模式

CREATE TABLE users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_users_username ON users (username);

CREATE TABLE conversations (
    id CHAR(36) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_updated (user_id, updated_at)
);

CREATE TABLE messages (
    id CHAR(36) PRIMARY KEY,
    conversation_id CHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    embedding_ref VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    INDEX idx_conv_time (conversation_id, created_at)
);

CREATE TABLE tool_calls (
    id CHAR(36) PRIMARY KEY,
    message_id CHAR(36) NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    params LONGTEXT,
    result LONGTEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE
);

CREATE INDEX idx_tool_calls_msg ON tool_calls (message_id);

CREATE TABLE skill (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(128) NOT NULL,
    description TEXT,
    version VARCHAR(32) NOT NULL,
    category VARCHAR(64),
    tags LONGTEXT,
    trigger_words LONGTEXT,
    icon_url VARCHAR(512),
    storage_path VARCHAR(512) NOT NULL,
    file_size BIGINT,
    checksum VARCHAR(64),
    author_id VARCHAR(64),
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    is_official BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_skill_slug ON skill (slug);

CREATE TABLE skill_version (
    id VARCHAR(36) PRIMARY KEY,
    skill_id VARCHAR(36) NOT NULL,
    version VARCHAR(32) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    file_size BIGINT,
    checksum VARCHAR(64),
    changelog TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES skill (id) ON DELETE CASCADE,
    UNIQUE KEY uk_skill_version (skill_id, version)
);

CREATE TABLE user_skill (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    skill_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    local_version VARCHAR(32),
    server_version VARCHAR(32),
    is_external BOOLEAN NOT NULL DEFAULT FALSE,
    external_path VARCHAR(512),
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skill (id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_skill (user_id, skill_id),
    INDEX idx_user_skill_user (user_id)
);
