# v0.7 — 技能工具扩展 + 工作空间（Workspace）

本版本对应 [开发路线图.md](../计划/开发路线图.md#v07--技能工具系统扩展原史诗③顺延--工作空间workspace)：**工具企业化能力**与 [工作空间系统设计](../workspace/workspace-design.md) 分阶段落地。

## 文档索引

| 文档 | 说明 |
|------|------|
| [PRD.md](./PRD.md) | 范围、与 HITL 协同的安全策略、Workspace 验收引用 |
| [接口文档.md](./接口文档.md) | Workspace Phase 3 REST（`/api/v1/workspace/**`） |
| [部署文档.md](./部署文档.md) | Flyway V9、配置与验收 |
| [workspace-design.md](../workspace/workspace-design.md) | 工作空间权威设计（Phase 1～3） |
| [skill-system-design.md](../workspace/skill-system-design.md) | 技能系统（市场、渐进式披露、客户端执行）权威设计 |

## 代码进展（抢跑 Phase 1 子集）

以下已合入 `agent-jingu3-cs-server`（可与路线图「未开始」并存，以代码为准迭代文档）：

- `jingu3.workspace.*`：`WorkspaceManager` + `DefaultWorkspaceManager`、`Workspace` / `WorkspaceStats`，`WorkspaceFileService` + `DefaultWorkspaceFileService` + `PathValidator`
- 配置：`jingu3.workspace.enabled`、`root-dir`、`max-file-size-mb`、`default-quota-mb`
- **ToolRegistry** 工具：`workspace_read_file`、`workspace_list_files`、`workspace_write_file`（关闭 `jingu3.workspace.enabled=false` 则不注册）

### Phase 2（进程沙箱）

- 配置：`jingu3.workspace.sandbox.enabled`（默认 `false`）、`max-timeout-seconds`、`max-output-chars`、`max-code-chars`、`python-command`、`node-command`（见 `application.yml`）
- `ProcessSandboxExecutor` + 工具 **`workspace_execute_code`**：在用户工作空间根下执行 Python / JavaScript（`code` 或 `relativePath`）；需同时满足 `jingu3.workspace` 可用且沙箱开启

### Phase 3（配额、执行历史、REST）

- Flyway **V9**：`workspace`、`workspace_execution`（见 [物化清单](../设计/系统数据存储-物化清单与测试数据.md)）
- 写入配额：`default-quota-mb` 为 0 时不校验；否则 `writeFile` 前按当前目录占用 + 新内容校验
- 沙箱执行落库：`jingu3.workspace.execution-history-enabled`（默认 `true`）；`execution-history-snippet-max-chars`、`execution-history-list-limit`
- REST（`jingu3.workspace.rest-api-enabled`，默认 `true`）：`GET/DELETE /api/v1/workspace`、`POST /api/v1/workspace/reset`、`GET /api/v1/workspace/stats`、`GET /api/v1/workspace/executions?limit=`

### 技能市场（只读 MVP）

- 表：**V6** `skill`（见 [物化清单](../设计/系统数据存储-物化清单与测试数据.md)）；种子示例见 [`dev_seed.sql`](../data/seed/dev_seed.sql)
- 配置：`jingu3.skill.api-enabled`、`jingu3.skill.list-max-size`
- REST：**`GET /api/v1/skills`**、**`GET /api/v1/skills/{slug}`**、**`GET /api/v1/skills/subscriptions`** — 公开目录 / 按 slug / 当前用户订阅联表（不含 `storage_path`）；下载 URL / MinIO 后续迭代

## 工序清单（发布前勾选）

- [x] README.md
- [x] PRD.md（初稿）
- [ ] 可行性分析.md
- [ ] 概要设计.md
- [ ] 详细设计.md
- [x] 接口文档.md（Workspace REST；OpenAPI 见 [docs/v0.5/openapi.yaml](../v0.5/openapi.yaml)）
- [ ] 单测与集成测（补充工具与 Controller 层）
- [x] 部署文档.md（初稿）
