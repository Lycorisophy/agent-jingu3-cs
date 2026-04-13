# v0.5 PRD — Cron / HITL / State / Agent Team 边界

## 1. 背景

[行动模式实现差距分析.md](../设计/行动模式实现差距分析.md) §3.6～3.8：Cron、State、HITL 当前为占位；Agent Team 仅 Leader + 单 Specialist。本版本交付 **持久化与可调度的横切能力** 及 **可验收的审批闭环**，并对 Agent Team 做**边界内**增强。

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

- `pending_approval` 存储结构 + REST：列表、批准、拒绝。
- 与 `conversationId` / `taskId` 关联（字段以详细设计为准）。

### 3.3 State

- 关系库或等价存储中的 **task_states** 最小表；与 conversation 关联。
- 替换或增强现有进程内 `StateTrackingModeHandler` 读路径（写路径同步更新）。

### 3.4 Agent Team（增强边界）

- **本版本交付**：显式多步消息结构（如 `List<AgentMessage>`）+ 合成一步；或 1 Leader + N 轮 Specialist 串行，**N 上限配置化**。
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
