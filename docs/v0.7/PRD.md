# v0.7 PRD — 技能扩展 + Workspace（初稿）

## 1. 背景

路线图将 **技能工具系统扩展** 与 **工作空间** 合入 v0.7：在 v0.3 工具 MVP 上增强发现、Schema、客户端工具与安全分级；工作空间按 [workspace-design.md](../workspace/workspace-design.md) 交付隔离文件与沙箱执行能力；技能市场与客户端执行见 [skill-system-design.md](../workspace/skill-system-design.md)。

## 2. 与 HITL 的协同（Phase 2 安全评审结论）

| 风险 | 缓解策略 |
|------|----------|
| `workspace_write_file` / 未来 `execute_code` 破坏数据或越权 | 路径限制在 `PathValidator` + 用户子目录；**生产环境**对写/执行类工具要求 **HITL 审批** 或配置开关（见 [详细设计.md](./详细设计.md) 第 4 节） |
| 模型误调用高危工具 | v0.7 **安全分级**：低危只读默认开启；写/执行与 **hitl_approval** 队列联动（`request_human_approval` 等待 `APPROVED` 后再执行，见 backlog） |
| 单用户 `001` 阶段 | 与路线图一致：物理隔离以 `jingu3.user.id` 子目录实现；多用户强约束随 v0.8 |

**评审记录**：本 PRD 将「Phase 2 与 HITL 协同」列为**必选设计输入**；目标态与现状对照见 `docs/v0.7/详细设计.md` 第 4 节（`hitl_approval` 与工具 `toolId` 的硬联动仍待后续迭代）。

## 3. Workspace 验收（引用设计文档）

- **Phase 1**：`WorkspaceManager` / `WorkspaceFileService` / 路径安全 / 工具注册 — **已落地**（见 [README.md](./README.md)）。
- **Phase 2**：`ProcessSandboxExecutor`、超时与资源限制、`workspace_execute_code`（Python/JS）— **已落地**（`jingu3.workspace.sandbox.enabled`）。
- **Phase 3**：`workspace` / `workspace_execution`、配额、`/api/v1/workspace/**` REST — **已落地**。

## 4. 技能扩展验收（初稿）

- **市场元数据只读**：`GET /api/v1/skills`、`GET /api/v1/skills/{slug}`、`GET /api/v1/skills/subscriptions` — **已落地**（见 [接口文档.md](./接口文档.md)）；对象存储下载与写接口仍待迭代。
- JSON Schema 覆盖工具描述（范围以详细设计为准）。
- 高危工具与 HITL 或确认流程可配置。

## 5. 修订记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-04-13 | 0.1 | 初稿：HITL 协同表 + Phase 引用 |
| 2026-04-13 | 0.2 | Workspace Phase 1～3 与技能只读 REST 与文档对齐 |
| 2026-04-13 | 0.3 | 详细设计定稿；PRD 评审记录与 HITL 表表述对齐 |
