-- 用户原始提示词密文审计（AES-256-GCM，密钥由配置提供）；与 conversations 无强制外键，便于缺省会话占位

CREATE TABLE user_prompt_cipher (
    id CHAR(36) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    conversation_id CHAR(36),
    ciphertext_b64 LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_created (user_id, created_at)
);
