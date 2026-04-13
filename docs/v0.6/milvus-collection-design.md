# Milvus Collection 设计 — jingu3 记忆向量

本文与实现类 [`MilvusMemoryVectorService`](../../agent-jingu3-cs-server/src/main/java/cn/lysoy/jingu3/memory/vector/MilvusMemoryVectorService.java) 对齐；集合名默认 `jingu3_memory`，可通过 `jingu3.milvus.collection-name` 覆盖。

## 1. 设计目标

| 目标 | 说明 |
|------|------|
| 与 MySQL 对账 | 主键与 `memory_entry.id` 一致，便于删除、排错与 `memory_embedding` 表同步 |
| 多租户隔离 | 检索时按 `user_id` 过滤（当前单用户 `001`，未来多用户可直接复用） |
| 可换嵌入模型 | 向量维度由 `jingu3.memory.embedding-dimension` 或首次 Ollama 探测决定；**换模型需新建集合并迁移** |
| 简单可运维 | MVP 单 Collection、FLAT 索引；数据量增大后再上 IVF/HNSW |

## 2. Schema（当前 MVP）

| 字段名 | Milvus 类型 | 约束 | 说明 |
|--------|-------------|------|------|
| `memory_entry_id` | Int64 | **主键**，非 auto-id | 等于 `memory_entry.id`；插入时必须显式提供 |
| `user_id` | VarChar(64) | 标量，过滤用 | 与 `memory_entry.user_id` 一致 |
| `embedding` | FloatVector | `dim = 模型维度` | Ollama `/api/embeddings` 输出；与 `jingu3.memory.embedding-model` 一致 |

**嵌入文本（代码侧，非 Milvus 列）**：`summary + "\n" + body`，与写入时一致，检索时用用户当前 query 量化的向量做 ANN。

## 3. 索引与度量

| 项 | 当前选择 | 说明 |
|----|----------|------|
| 向量索引 | `FLAT` | 暴力检索，适合万级以内实体；实现简单、召回全 |
| 度量 | `COSINE` | 与常见归一化嵌入习惯一致；若模型输出未归一化，可评估改为 `L2` |
| 标量索引 | 依赖 Milvus 默认 | `user_id` 在 `expr` 中过滤；数据量大时可显式建标量索引（视 Milvus 版本能力） |

**数据量上来后**（例如单用户 >10w 条记忆向量）：将 `embedding` 索引改为 **IVF_FLAT / IVF_SQ8 / HNSW**（需调 `nlist`、`nprobe` 或 HNSW `M`、`efConstruction`），并在 `SearchParam` 中配置对应 `params`。

## 4. 检索表达式

- 过滤：`` user_id == "{userId}" ``（注意对引号转义，避免注入）。
- TopK：`jingu3.memory.retrieval-top-k`（默认 5）。
- 结果主键：使用 `getIDScore` 中的 `longID`，即 `memory_entry_id`；再回到 MySQL 拉 `summary`/`body` 拼「参考记忆」。

## 5. 与关系库的配合

```
memory_entry (MySQL)     memory_embedding (MySQL)        Milvus collection
-------------------     ------------------------        -----------------
id (PK)          <----   memory_entry_id (PK)            memory_entry_id (PK)
user_id                  updated_at (同步标记)          user_id
summary/body             （可选：仅表示已向量化）        embedding
```

- 删除/更新记忆时：理想情况应对 Milvus 执行 **delete by expr**（`memory_entry_id in [...]`）并删 `memory_embedding` 行；当前 MVP 以新增为主，后续可补 **upsert/delete** 流水线。

## 6. 演进建议（未实现，供评审）

| 扩展字段 | 类型 | 用途 |
|----------|------|------|
| `kind` | VarChar(16) | `FACT` / `EVENT`，检索时 `user_id && kind == "FACT"` |
| `conversation_id` | VarChar(128) | 会话级记忆过滤 |
| `text_preview` | VarChar(1024) | 冗余短文本，减少回表（注意与 MySQL 一致性） |
| `created_at` | Int64 | Unix 秒，便于时间衰减或排序重排 |

新增字段需 **新建 Collection 或做 schema migration**（Milvus 2.x 对改列支持有限），并安排双写/回填。

## 7. 运维注意

- **首次启动**：若集合不存在，服务会根据当前嵌入维度 **自动建表**；生产环境建议由平台侧 **预创建** 同名集合并与配置维度严格一致，避免多实例竞态建表。
- **换模型**：维度变化必须 **新 collection + 全量重嵌入**，不可原地改 `dim`。
- **一致性**：`insert` 后代码中 `flush`；检索前需 collection 处于 **Loaded** 状态（实现里已 `loadCollection`）。

## 8. 修订记录

| 日期 | 说明 |
|------|------|
| 2026-04-13 | 初稿：与 `MilvusMemoryVectorService` MVP 三字段 schema 一致 |
