# v0.7 PRD — 技能扩展 + Workspace（初稿）

## 1. 背景

路线图将 **技能工具系统扩展** 与 **工作空间** 合入 v0.7：在 v0.3 工具 MVP 上增强发现、Schema、客户端工具与安全分级；按 [workspace-design.md](../workspace/workspace-design.md) 交付隔离文件与沙箱执行能力。

## 2. 与 HITL 的协同（Phase 2 安全评审结论）

| 风险 | 缓解策略 |
|------|----------|
| `workspace_write_file` / 未来 `execute_code` 破坏数据或越权 | 路径限制在 `PathValidator` + 用户子目录；**生产环境**对写/执行类工具要求 **HITL 审批** 或配置开关（详细设计定稿） |
| 模型误调用高危工具 | v0.7 **安全分级**：低危只读默认开启；写/执行与 **hitl_approval** 队列联动（`request_human_approval` 等待 `APPROVED` 后再执行，见 backlog） |
| 单用户 `001` 阶段 | 与路线图一致：物理隔离以 `jingu3.user.id` 子目录实现；多用户强约束随 v0.8 |

**评审记录**：本 PRD 将「Phase 2 与 HITL 协同」列为**必选设计输入**；具体字段与 API 在 `docs/v0.7/详细设计.md` 定稿时引用 `hitl_approval` 与工具 `toolId`。

## 3. Workspace 验收（引用设计文档）

- **Phase 1**：`WorkspaceManager` / `WorkspaceFileService` / 路径安全 / 工具注册 — **代码已部分落地**（见 [README.md](./README.md)）。
- **Phase 2**：`SandboxExecutor`、超时与资源限制、`execute_code` — 待实现。
- **Phase 3**：`execution_history`、配额、**`/api/v1/workspace/...`** REST — 待实现。

## 4. 技能扩展验收（初稿）

- JSON Schema 覆盖工具描述（范围以详细设计为准）。
- 高危工具与 HITL 或确认流程可配置。

## 5. 修订记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-04-13 | 0.1 | 初稿：HITL 协同表 + Phase 引用 |
