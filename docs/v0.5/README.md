# v0.5 — 横切能力（Cron / HITL / State / Agent Team）

本目录按[全局文档工序](../计划/开发路线图.md#全局--文档工序每个-v0x-发布前完成)补齐 PRD → 部署文档；与 [开发路线图 v0.5 节](../计划/开发路线图.md#v05--cron--hitl--state-持久化--agent-team-增强边界差距整改--横切) 对齐。

## 后续增强（Backlog）

- [BACKLOG-横切增强.md](./BACKLOG-横切增强.md)（HITL 工具/续跑、DST 与 Handler、Cron 周期）

## 关联设计

| 文档 | 说明 |
|------|------|
| [v0.5-横切能力极简设计.md](../设计/v0.5-横切能力极简设计.md) | B1 流式约定、阶段 C、B2/B3/B4 表字段与 REST 占位 |
| [Cron与HITL模块设计草案.md](../设计/Cron与HITL模块设计草案.md) | 历史草案，与极简设计对照合并 |
| [行动模式实现差距分析.md](../设计/行动模式实现差距分析.md) 第八章 | 现状 vs 规范 |

## 已落地（代码/配置）

- [x] B1 显式 Plan / Agent Team 守门（`EXPLICIT_GUARD`、`guardUserNotice`、流式 `BLOCK` 前缀）
- [x] `jingu3.routing.explicit-mode-guard-enabled` 开关（默认 `true`，关闭则显式模式直通）
- [x] `ModeRoutingPreamble` 覆盖 `EXPLICIT_GUARD`；Cron / State / HITL 的 `stream` 实现
- [x] **Cron MVP**：Flyway `V1__scheduled_task`；`ScheduledTaskPoller`；**`/api/v1/cron/tasks`**
- [x] **HITL MVP**：Flyway `V2__hitl_approval`；**`/api/v1/hitl`**（创建、`pending`、approve/reject）
- [x] **DST 占位**：Flyway `V3__dialogue_state`；**`/api/v1/dst/{conversationId}`**（GET/PATCH/confirm）
- [x] **Agent Team**：`jingu3.engine.agent-team.max-specialist-rounds` + 多轮 Specialist + **synthesize**；流式步 `leader` / `specialist_{n}` / `synthesize`

## 关闭本版本后的下一立项

- **v0.6**：记忆系统（事件/事实/检索），见路线图与 [记忆知识系统数据库选型分析.md](../设计/记忆知识系统数据库选型分析.md)。
- **v0.7**：技能工具扩展 + [工作空间系统设计](../workspace/workspace-design.md) + [技能系统设计](../workspace/skill-system-design.md)（Workspace / Skill Phase 分阶段），见路线图 v0.7 节。

## 工序清单（发布前勾选）

- [x] PRD.md
- [x] 可行性分析.md
- [x] 概要设计.md
- [x] 详细设计.md
- [x] 接口文档.md + [openapi.yaml](./openapi.yaml)（摘要，可与 Springdoc 生成结果后续对齐）
- [x] [验收清单.md](./验收清单.md)（PRD AC 手工步骤）
- [x] 单测与集成测（持久化层 **MyBatis-Plus** + `@SpringBootTest`；全量 `mvn test` / `mvn verify` 见 CI）
- [x] 部署文档.md
