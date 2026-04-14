-- v0.6：事实记忆时间维度与确认时刻（PRD 永驻/长期/短期占位；确认流 MVP）

ALTER TABLE fact_metadata ADD COLUMN temporal_tier VARCHAR(16) NOT NULL DEFAULT 'SHORT_TERM';

ALTER TABLE fact_metadata ADD COLUMN confirmed_at TIMESTAMP NULL;

-- 历史 FACT 无 fact_metadata 行时补一行默认短期、未确认
INSERT INTO fact_metadata (memory_entry_id, tag, temporal_tier, confirmed_at)
SELECT m.id, NULL, 'SHORT_TERM', NULL
FROM memory_entry m
WHERE m.kind = 'FACT'
  AND NOT EXISTS (SELECT 1 FROM fact_metadata f WHERE f.memory_entry_id = m.id);
