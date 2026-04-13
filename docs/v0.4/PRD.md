# v0.4 PRD — Plan 执行器工具化 + Workflow 节点扩展

## 1. 背景

[行动模式实现差距分析.md](../设计/行动模式实现差距分析.md) 指出：Plan-and-Execute 已有 Planner/Executor/Replanner 骨架，但 **Executor 仍为纯 LLM**；Workflow 仅有「顺序 LLM 节点」与简单 fallback。本版本在 **v0.3 工具层** 之上，让子任务与工作流节点可调用真实工具。

## 2. 用户故事

1. **作为** 用户 **我希望** 复杂任务被拆成多步且某步可调用计算器/检索等工具 **以便** 计划可执行、结果可核验。
2. **作为** 配置方 **我希望** 工作流 JSON 能声明 TOOL 节点 **以便** 与 LLM 节点组成固定流水线。

## 3. 范围

### 3.1 包含

- `PlanAndExecuteModeHandler`：子任务执行路径可调用 v0.3 `ToolExecutor`（策略：每子任务先判定是否需要工具，见详细设计）。
- `WorkflowDefinition` / `WorkflowNode`：增加 **节点类型** 字段（至少 `LLM`、`TOOL`）；`WorkflowModeHandler` 按类型分支；**顺序执行**为主。
- 兼容现有无类型 JSON（默认视为 LLM 节点）。

### 3.2 不包含（可列后续里程碑）

- 并行节点、复杂条件路由（Evaluator-Optimizer 等）可单列 v0.4.x 或 v0.9+。
- 可视化编排器。

## 4. 验收标准

| 编号 | 验收项 |
|------|--------|
| AC-1 | 某 Plan 子任务执行路径上可观察到工具调用与结果进入最终拼接 |
| AC-2 | 一份示例 `workflows/*.json` 含 TOOL 节点且可端到端跑通 |
| AC-3 | 回归：原纯 LLM Plan 与 Workflow 行为不因缺省类型而破坏 |

## 5. 修订记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-04-12 | 0.1 | 初稿 |
