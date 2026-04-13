-- 可选：演示数据（手动执行，勿与自动化迁移混跑多次导致主键冲突）
-- MySQL 8 示例（务必 utf8mb4，避免中文乱码）：
--   mysql --default-character-set=utf8mb4 -uUSER -p jingu3 < docs/data/test-data/sample_seed.sql
-- 执行前请先跑完 migration/ 下 V1～V4，保证存在 memory_entry 表。

INSERT INTO memory_entry (user_id, kind, summary, body, created_at, updated_at)
VALUES ('001', 'EVENT', '演示事件', '种子正文', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 若需 fact_metadata，可在上一条之后执行（依赖 LAST_INSERT_ID）：
-- INSERT INTO fact_metadata (memory_entry_id, tag) VALUES (LAST_INSERT_ID(), 'demo');
