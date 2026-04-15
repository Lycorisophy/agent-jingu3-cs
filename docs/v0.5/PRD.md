# v0.5 PRD — Cron / HITL / State / Agent Team 边界

## 1. 背景

[行动模式实现差距分析.md](../设计/行动模式实现差距分析.md) §3.6～3.8：Cron、State、HITL 当前为占位；Agent Team 为 Leader + 多轮 Specialist + 合成（见 §3.4）。本版本交付 **持久化与可调度的横切能力** 及 **可验收的审批闭环**，并对 Agent Team 做**边界内**增强。

## 2. 用户故事

1. **作为** 用户 **我希望** 创建定时任务并持久保存 **以便** 重启后仍存在。
2. **作为** 审批人 **我希望** 在队列中看到待审批项并批准或拒绝 **以便** 会话可在批准后继续（回调策略见详细设计）。
3. **作为** 用户 **我希望** 查看任务级状态（进行中/完成/失败）**以便** 对齐指南 §9。
4. **作为** 产品 **我希望** Agent Team 至少支持多轮 Specialist 或显式消息列表 **以便** 与「多专家」表述一致（本版本不承诺分布式多进程 Agent）。

## 3. 范围

### 3.1 Cron

- 持久化存储（与 [Cron与HITL模块设计草案.md](../设计/Cron与HITL模块设计草案.md) 一致方向）。
- Spring `@Scheduled` 或 Quartz 触发 `executeJob` 回调现有引擎管线（最小：调用一次与 `message` 绑定的逻辑）。

### 3.2 HITL

- 表 **`hitl_approval`**（见 [v0.5-横切能力极简设计.md §3](../设计/v0.5-横切能力极简设计.md)）+ REST：**`/api/v1/hitl`**（创建、`pending` 列表、approve/reject）。
- 与 `conversationId`、`run_id` 关联；`resolver_user_id` 使用当前种子用户。

### 3.3 State / DST（占位）

- 表 **`dialogue_state`**：按 `conversation_id` 唯一存储侧栏 JSON 状态（`state_json`、`revision`）。
- REST：**`/api/v1/dst/{conversationId}`**（GET/PATCH）与 **`POST .../confirm`**。
- **本版本不强制**改写 `StateTrackingModeHandler` 为读写该表；模型门控与 `state_digest` 卡片可后续迭代。

### 3.4 Agent Team（增强边界）

- **本版本交付**：1 Leader + **N 轮** Specialist 串行（`jingu3.engine.agent-team.max-specialist-rounds`）+ **一次合成** LLM 生成用户可见答复；同步/流式均多一步可观测轨迹（含 `synthesize`）。
- **专员轮**：`jingu3.tool.enabled=true` 时与 Ask 相同走内置工具 JSON 路由（可多一次工具执行再汇总）；`false` 时仍为单段生成。
- **职责**：专员输出为纯文本执行结果，**不**承担需用户点击的确认卡片、**不**引导或冒充创建定时任务；若涉及人在环或定时，由主协调在**合成答复**中向用户说明或引导。
- **不交付**：去中心化多 Agent 运行时、跨机消息总线。

## 4. 验收标准

| 编号 | 验收项 |
|------|--------|
| AC-1 | 创建 Cron 后重启服务仍存在且可被触发 |
| AC-2 | HITL：至少一条记录 `pending → approved/rejected` 可查 |
| AC-3 | State：可按 task/conversation 查询状态，与引擎写操作一致 |
| AC-4 | Agent Team：日志或 API 可观测多段角色输出（若纳入本版本范围） |

## 5. 修订记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-04-12 | 0.1 | 初稿 |
| 2026-04-12 | 0.2 | 对齐已实现 API：`hitl_approval`、`dialogue_state`、Agent Team 多轮+合成 |
| 2026-04-13 | 0.3 | Agent Team：专员轮复用 Ask 工具路由；主/子职责（卡片确认、定时任务）提示词与文档 |
