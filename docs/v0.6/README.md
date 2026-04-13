# v0.6 — 记忆系统

本版本对应 [开发路线图.md](../计划/开发路线图.md#v06--记忆系统原史诗②顺延) 史诗③（记忆），技术选型与组件优先级以 [记忆知识系统数据库选型分析.md](../设计/记忆知识系统数据库选型分析.md) **§7.1** 为重要输入。

## 文档索引

| 文档 | 说明 |
|------|------|
| [PRD.md](./PRD.md) | 用户故事与验收标准（初稿） |
| [里程碑切片.md](./里程碑切片.md) | 从选型报告拆出的分阶段交付与**首个可合并增量**建议 |
| （后续）可行性分析 / 概要设计 / 详细设计 / 接口文档 / 部署文档 | 按[全局工序](../计划/开发路线图.md#全局--文档工序每个-v0x-发布前完成)补齐 |

### M1 已实现（代码）

- **Flyway**：`V4__memory_m1.sql` — `memory_entry`、`fact_metadata`。
- **包**：`cn.lysoy.jingu3.memory`（实体、Mapper、`MemoryService` / `DefaultMemoryService`）。
- **配置**：`jingu3.memory.api-enabled`（默认 `true`）、`jingu3.memory.max-list-size`（默认 `100`）。
- **实验 API**（生产可关 `api-enabled`；**对话注入**见下节 M4）：
  - `POST /api/v1/memory/entries` — 请求体 JSON：`userId`、`kind`（`FACT`|`EVENT`）、可选 `summary`、`body`；`FACT` 时可带 `factTag` 写入 `fact_metadata.tag`。
  - `GET /api/v1/memory/entries?userId=...` — 按用户倒序列表（受 `max-list-size` 限制）；可选 **Redis** 缓存（`jingu3.redis.enabled=true`）。

### M3 / M4 已实现（增量）

- **Redis**：`jingu3.redis.enabled`（默认 `false`）；启用后注册 `StringRedisTemplate`，记忆列表带 TTL 缓存。
- **Milvus + Ollama 嵌入**：`jingu3.milvus.enabled`（默认 `false`）；`OllamaEmbeddingClient` 调用 `jingu3.ollama.base-url` + `/api/embeddings`；`jingu3.memory.embedding-model`（默认 `qwen3-embedding:8b`）。
- **Flyway `V5__memory_embedding.sql`**：标记已写入向量的 `memory_entry_id`。
- **对话注入**：`jingu3.memory.injection-enabled`（默认 `false`）；为 `true` 且 Milvus 启用时，`ChatService` / `ChatStreamService` / `ModePlanExecutor` 在构造 `ExecutionContext` 前拼接参考记忆（意图路由仍用用户原文）。
- **本地联调**：[`application-local.yml.example`](../../agent-jingu3-cs-server/src/main/resources/application-local.yml.example)。

## 工序清单（发布前勾选）

- [x] README.md（本目录入口）
- [x] PRD.md（初稿）
- [x] 里程碑切片.md
- [ ] 可行性分析.md
- [ ] 概要设计.md
- [ ] 详细设计.md
- [ ] 接口文档.md / openapi
- [ ] 服务端实现（`agent-jingu3-cs-server`）（**M1 子集已合入**：见上节）
- [ ] 单测与集成测（**M1**：`memory` 包仓库/服务单测已加；全量与集成测随后续里程碑补充）
- [ ] 部署文档.md
