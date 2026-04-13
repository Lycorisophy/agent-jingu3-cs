# v0.7 — 技能工具扩展 + 工作空间（Workspace）

本版本对应 [开发路线图.md](../计划/开发路线图.md#v07--技能工具系统扩展原史诗③顺延--工作空间workspace)：**工具企业化能力**与 [工作空间系统设计](../workspace/workspace-design.md) 分阶段落地。

## 文档索引

| 文档 | 说明 |
|------|------|
| [PRD.md](./PRD.md) | 范围、与 HITL 协同的安全策略、Workspace 验收引用 |
| [workspace-design.md](../workspace/workspace-design.md) | 工作空间权威设计（Phase 1～3） |

## 代码进展（抢跑 Phase 1 子集）

以下已合入 `agent-jingu3-cs-server`（可与路线图「未开始」并存，以代码为准迭代文档）：

- `jingu3.workspace.*`：`WorkspaceFileService` + `DefaultWorkspaceFileService` + `PathValidator`
- 配置：`jingu3.workspace.enabled`、`root-dir`、`max-file-size-mb`
- **ToolRegistry** 工具：`workspace_read_file`、`workspace_list_files`、`workspace_write_file`（关闭 `jingu3.workspace.enabled=false` 则不注册）

## 工序清单（发布前勾选）

- [x] README.md
- [x] PRD.md（初稿）
- [ ] 可行性分析.md
- [ ] 概要设计.md
- [ ] 详细设计.md
- [ ] 接口文档.md（Workspace REST 待 Phase 3）
- [ ] 单测与集成测（补充工具与 Controller 层）
- [ ] 部署文档.md
