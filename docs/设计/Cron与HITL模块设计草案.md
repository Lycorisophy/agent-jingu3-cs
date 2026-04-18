# Cron（§8）与 Human-in-the-Loop（§10）模块设计草案

对应《AI智能体行动模式设计指南》**§8 定时任务**、**§10 人在环**。当前对话 HTTP API **不直接选取** `CRON` / `HUMAN_IN_LOOP` 模式（见 `ActionModePolicy`）；独立模块用于与指南中的 **CronStore、调度器、审批队列** 对齐。

## 1. Cron 模块（指南 §8.1–§8.2）

### 1.1 能力

- **CronJob 持久化**：任务 id、调度类型（at / every / cron 表达式）、时区、启用状态、关联 Agent 配置或提示词模板。
- **调度运行时**：Spring `@Scheduled` 扫描或 Quartz，触发 `executeJob(jobId)`。
- **执行管线**：构造 `ExecutionContext`（或内部任务上下文）→ 调用 `ModeRegistry` / 指定 Handler → 记录结果与下次触发时间。
- **通知**：可选 Webhook/站内信（依赖用户系统史诗）。

### 1.2 API 草图（REST）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/internal/cron/jobs` | 创建任务 |
| GET | `/api/v1/internal/cron/jobs` | 列表 |
| DELETE | `/api/v1/internal/cron/jobs/{id}` | 删除 |

（路径前缀 `internal` 表示需服务账号或后续 JWT。）

### 1.3 与引擎关系

- 定时触发时**不经过** `IntentRouter` 的 HTTP 路径，直接注入会话/用户常量执行任务。

## 2. HITL 模块（指南 §10）

### 2.1 能力

- **待审批记录**：`approvalId`、会话/任务引用、摘要、超时时间、状态（PENDING/APPROVED/REJECTED）。
- **人工决策 API**：审批通过/驳回；通过后 **回调** 继续同一逻辑会话（resume token 或二次 `POST /chat` 带 `approvalToken`）。

### 2.2 API 草图

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/approvals/pending` | 待办列表 |
| POST | `/api/v1/approvals/{id}/resolve` | body: `{ "decision": "APPROVE" \| "REJECT" }` |

### 2.3 与引擎关系

- 高风险工具调用前由 `HUMAN_IN_LOOP` 或工具中间件写入 pending，阻塞直至审批回调。

## 3. 版本建议

> **以 [开发路线图 v2.0](../计划/开发路线图.md) 为准**：Cron / HITL / State 等横切能力目标在 **v0.5**；**多用户与鉴权**在 **v1.0**（原 v0.8 顺延）。

- **Cron**：与 State、Agent Team 等一并纳入路线图 **v0.5** 横切交付；此前可在 v0.4+ 以最小实现迭代。
- **HITL**：完整待办/鉴权与 **多用户** 强相关，以 **v1.0** 为完备目标；**v0.5** 可先进程内队列 + 单用户演示，与路线图一致。

## 4. 关联代码（当前）

- `CronModeHandler`、`HumanInLoopModeHandler`：演示级字符串，供未来内部调用或测试注册表保留。
