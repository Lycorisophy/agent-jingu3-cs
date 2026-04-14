# v0.6 — 记忆系统

本版本对应 [开发路线图.md](../计划/开发路线图.md#v06--记忆系统原史诗②顺延) 中的记忆史诗。**存储终局**以 [记忆知识系统数据库选型分析第二版.md](../设计/记忆知识系统数据库选型分析第二版.md) 与 [系统数据存储方案.md](../设计/系统数据存储方案.md) 为准；**后续迭代顺序**见路线图 **v0.6-A / v0.6-B / v0.6-C**。第一版 [记忆知识系统数据库选型分析.md](../设计/记忆知识系统数据库选型分析.md) 可作历史对照。

## 文档索引

| 文档 | 说明 |
|------|------|
| [PRD.md](./PRD.md) | 用户故事与验收标准 |
| [可行性分析.md](./可行性分析.md) | 依赖、风险、结论 |
| [概要设计.md](./概要设计.md) | 模块划分、数据流、持久化边界 |
| [详细设计.md](./详细设计.md) | 表、API、注入与 Milvus 行为 |
| [接口文档.md](./接口文档.md) | 记忆 REST；OpenAPI 片段见 [v0.5/openapi.yaml](../v0.5/openapi.yaml) |
| [部署文档.md](./部署文档.md) | 构建、Flyway、外部依赖与验收步骤 |
| [里程碑切片.md](./里程碑切片.md) | 分阶段交付（已对齐第二版） |
| [milvus-collection-design.md](./milvus-collection-design.md) | **Milvus 记忆集合**字段、索引、过滤与演进建议 |
| [系统数据存储-物化清单与测试数据.md](../设计/系统数据存储-物化清单与测试数据.md) | 全站 DDL/索引/桶/集合参考与种子 |

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

### M2 / v0.6-C（部分已实现）

- **Elasticsearch**：`jingu3.elasticsearch.enabled`（默认 `false`）；`POST /api/v1/events`、`GET /api/v1/events/search`；索引名 `jingu3.elasticsearch.index-events`（默认 `jingu3-events`）；映射见服务端 `resources/elasticsearch/jingu3-events-index.json`（与仓库 `docs/data/elasticsearch/events-index.json` 可并存：后者可含 IK）。
- **依赖降级**：`GET /api/v1/events/search` 在 ES 异常时返回空列表并打 WARN；`POST` 失败为 **503** / `AG_50301`；记忆向量注入见 `MilvusMemoryRetrievalService` 与 `MemoryAugmentationService` 外层兜底。
- **未纳入本批**：对话回合自动写 ES、Milvus `jingu3_event_vectors`、Neo4j `EVENT_LINK`。

## 工序清单（发布前勾选）

- [x] README.md（本目录入口）
- [x] PRD.md（初稿）
- [x] 里程碑切片.md
- [x] 可行性分析.md（**v0.6-A**）
- [x] 概要设计.md（**v0.6-A**）
- [x] 详细设计.md（**v0.6-A**）
- [x] 接口文档.md；OpenAPI 路径已含于 [v0.5/openapi.yaml](../v0.5/openapi.yaml)（**v0.6-A**）
- [ ] 服务端实现（`agent-jingu3-cs-server`）（**M1～M4 已合入**；**v0.6-B/C** 见路线图）
- [ ] 单测与集成测（`memory` 包单测已有；ES/集成测随 **v0.6-C**）
- [x] 部署文档.md（**v0.6-A**）
