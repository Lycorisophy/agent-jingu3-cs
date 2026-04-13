# v0.5+ 横切增强 Backlog（非当前里程碑阻塞项）

对应 [v0.5-横切能力极简设计.md](../设计/v0.5-横切能力极简设计.md) 中尚未接入对话引擎或工具管线的能力。按产品优先级插队，建议在 **HITL 与 Workspace Phase 2** 联调之后逐步收口。

## HITL

- 内置工具 `request_human_approval`（或等价名）：写 `hitl_approval` 并返回 `approval_id`。
- 流式帧：`cardKind=action_approval` 或与 **BLOCK** 结构化约定对齐（见极简设计 §2 阶段 C）。
- 批准后 **同 `run_id` 续跑**：与 ReAct / Plan `ToolStepService` 挂起/恢复状态机衔接。

## DST / State

- `StateTrackingModeHandler` 读写 **`dialogue_state`** 表（与 `/api/v1/dst` 同源）。
- 工具 `begin_state_tracking` / `update_dialogue_state`；检查点 **CARD** `state_digest`。

## Cron

- 由 `cron_expression` **重算 `next_run_at`**（或引入 Quartz），支持周期任务而非仅单次 MVP。
- 工具 `create_scheduled_task` 写库，与 REST 双路径（极简设计 §5）。

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-04-13 | 初稿：与后续工作规划 backlog 一致 |
