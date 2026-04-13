-- 开发/联调用种子数据（手动执行，非 Flyway 自动跑）
-- 前置：已执行 Flyway V1～V6；与 docs/设计/系统数据存储-物化清单与测试数据.md 一致

-- 单用户（与 jingu3.user.id 默认 001 对齐）；password_hash 为常见测试用 BCrypt("password")
INSERT INTO users (id, username, password_hash) VALUES
    ('001', 'user', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi');

-- 会话与消息
INSERT INTO conversations (id, user_id, title) VALUES
    ('aaaaaaaa-bbbb-4ccc-dddd-eeeeeeee0001', '001', '演示会话');

INSERT INTO messages (id, conversation_id, role, content, embedding_ref) VALUES
    ('bbbbbbbb-cccc-4ddd-eeee-ffffffff0001', 'aaaaaaaa-bbbb-4ccc-dddd-eeeeeeee0001', 'user', '你好，请总结项目记忆设计要点。', NULL),
    ('bbbbbbbb-cccc-4ddd-eeee-ffffffff0002', 'aaaaaaaa-bbbb-4ccc-dddd-eeeeeeee0001', 'assistant', '要点：MySQL 存元数据，ES 存事件全文，Milvus 存向量，Neo4j 存关系，MinIO 存大文件。', 'milvus:event:evt_demo_001');

INSERT INTO tool_calls (id, message_id, tool_name, params, result, status, finished_at) VALUES
    ('cccccccc-dddd-4eee-ffff-000000000001', 'bbbbbbbb-cccc-4ddd-eeee-ffffffff0002', 'workspace_read_file', '{"path":"README.md"}', '...', 'success', CURRENT_TIMESTAMP);

-- 技能（MinIO 前缀与 storage_path 一致）
INSERT INTO skill (id, name, slug, description, version, category, tags, trigger_words, storage_path, checksum, author_id, is_public, is_official, status) VALUES
    ('dddddddd-eeee-4fff-aaaa-bbbbbbbbbbb1', '示例 PDF 技能', 'demo-pdf-skill', '演示用技能', '1.0.0', 'doc', '["pdf"]', '["pdf","总结"]', 'skills/dddddddd-eeee-4fff-aaaa-bbbbbbbbbbb1/1.0.0/', 'd41d8cd98f00b204e9800998ecf8427e', '001', TRUE, TRUE, 'ACTIVE');

INSERT INTO skill_version (id, skill_id, version, storage_path, changelog) VALUES
    ('eeeeeeee-ffff-4aaa-bbbb-ccccccccccc1', 'dddddddd-eeee-4fff-aaaa-bbbbbbbbbbb1', '1.0.0', 'skills/dddddddd-eeee-4fff-aaaa-bbbbbbbbbbb1/1.0.0/', '初始版本');

INSERT INTO user_skill (id, user_id, skill_id, status, local_version, server_version) VALUES
    ('ffffffff-aaaa-4bbb-cccc-dddddddddddd1', '001', 'dddddddd-eeee-4fff-aaaa-bbbbbbbbbbb1', 'ACTIVE', '1.0.0', '1.0.0');

-- 记忆条目（与 Milvus memory_entry_id 对齐时需用固定 id；以下为 BIGINT 自增环境可改为去掉 id 由库生成）
INSERT INTO memory_entry (user_id, kind, summary, body) VALUES
    ('001', 'FACT', '默认栈', 'MySQL + Redis + ES + Milvus + Neo4j + MinIO');

-- 若需与 Milvus 对账，请记下上一条 memory_entry.id，再插入 memory_embedding 并写入向量侧同 id
