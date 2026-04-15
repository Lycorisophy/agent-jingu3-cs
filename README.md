一款 **C/S 架构**的智能体（Agent）应用框架，服务端负责核心推理与工具调度，客户端提供跨平台（Windows/macOS/Linux）交互界面并承担本地资源访问职责。

jingu3的名字取自紧箍咒/金箍棒/禁锢+三角形（具有稳定性）/不可能三角/三生万物/下面的三大

该框架核心三大系统：8大行动模式（包含意图场景失败）+多层记忆体系+技能系统

三大工程：提示词工程，上下文工程，驾驭工程

三大模型：嵌入模型、BERT类小模型、多模态MOE大模型

我就是要用互联网思维，企业级规范，JAVA生态来完成这一个大框架，目的就是开源

---

## 仓库布局（与 [docs/服务架构.md](docs/服务架构.md) §7 一致）

| 路径 | 说明 |
|------|------|
| [`pom.xml`](pom.xml) | Maven **父工程**（`packaging`：`pom`），聚合 `agent-jingu3-cs-server`，统一 `dependencyManagement`（含 **Spring Cloud BOM**，不默认引入 Cloud starter）与 PMD / SpotBugs 插件管理 |
| [`agent-jingu3-cs-server/`](agent-jingu3-cs-server/) | **唯一** Spring Boot 可运行模块；主类 `cn.lysoy.jingu3.Jingu3Application` |
| [`docs/`](docs/) | 规范、设计、各版本 `docs/v0.x/`、Flyway 源脚本 [`docs/data/migration/`](docs/data/migration) |
| [`.github/workflows/`](.github/workflows/) | CI：根目录 `mvn clean verify` |
| `.minimax/` | 与主产品解耦的工具链/技能示例，**不参与**服务端构建 |

---

## 文档索引

| 文档 | 说明 |
|------|------|
| [docs/规范/jingu3-开发规范.md](docs/规范/jingu3-开发规范.md) | 史诗顺序、版本文档流水线、`docs/{版本号}/` 约定、Ollama 与单用户 |
| [docs/计划/开发路线图.md](docs/计划/开发路线图.md) | v0.1+ 版本目标、功能与文档工序勾选（**单一事实来源**） |
| [docs/服务架构.md](docs/服务架构.md) | 总体架构与技术选型（与实现对齐处见文内版本表） |
| [docs/workspace/workspace-design.md](docs/workspace/workspace-design.md) | 工作空间（隔离目录、安全文件、沙箱与工具）权威设计 |
| [docs/workspace/skill-system-design.md](docs/workspace/skill-system-design.md) | 技能系统（市场、渐进式披露、客户端执行）权威设计 |
| [docs/workflow/workflow-design.md](docs/workflow/workflow-design.md) | 工作流**平台**扩展愿景（Flowable/BPMN）；与对话模式 `workflowId` 的 JSON 工作流区分 |
| [docs/v0.1/README.md](docs/v0.1/README.md) | v0.1 文档清单（史诗① 启动已交付） |
| [docs/v0.2/README.md](docs/v0.2/README.md) | v0.2 文档清单 |
| [docs/v0.3/README.md](docs/v0.3/README.md) | v0.3 文档清单（工具子系统 MVP + Ask/ReAct 真闭环） |
| [docs/v0.4/README.md](docs/v0.4/README.md) | v0.4 文档清单（Plan / Workflow 工具化等） |
| [docs/v0.5/README.md](docs/v0.5/README.md) | v0.5 文档清单（HITL、DST、Agent Team 等）；[OpenAPI 草案](docs/v0.5/openapi.yaml) |
| [docs/v0.6/README.md](docs/v0.6/README.md) | v0.6 文档清单（记忆系统） |
| [docs/v0.7/README.md](docs/v0.7/README.md) | v0.7 文档清单（技能与工作空间） |
| [docs/v0.8/README.md](docs/v0.8/README.md) | v0.8 文档清单（用户系统） |
| [docs/v0.9/README.md](docs/v0.9/README.md) | v0.9 框架横切增强（非 UAP 主线） |
| [docs/v0.10/README.md](docs/v0.10/README.md) | v0.10 UAP：工具与侧车 |
| [docs/v0.11/README.md](docs/v0.11/README.md) | v0.11 UAP：记忆与领域知识 |
| [docs/v0.12/README.md](docs/v0.12/README.md) | v0.12 UAP：意图策略与产品闭环 |
| [docs/设计/复杂系统未来势态量化预测统一智能体/复杂系统未来势态量化预测统一智能体jingu3架构的技术适配与落地方案.md](docs/设计/复杂系统未来势态量化预测统一智能体/复杂系统未来势态量化预测统一智能体jingu3架构的技术适配与落地方案.md) | UAP 技术适配与落地方案（v0.10～v0.12 设计索引） |

服务端入口：`agent-jingu3-cs-server/`，主类 `cn.lysoy.jingu3.Jingu3Application`。核心对话为 `POST /api/v1/chat`（SSE/WebSocket 流式见实现与客户端规范）；另含 HITL、对话状态 DST、Cron、健康检查等 **`/api/v1/...`** 能力。**接口以各版本 `docs/v0.x/接口文档.md`、路线图勾选及** [docs/v0.5/openapi.yaml](docs/v0.5/openapi.yaml) **等为补充**；v0.1 契约说明见 [docs/v0.1/接口文档.md](docs/v0.1/接口文档.md)。

**本地构建与门禁**（与 [CI](.github/workflows/ci.yml) 一致）：在仓库根执行 `mvn -B clean verify`（Reactor 构建 `agent-jingu3-cs-server`，含单测、PMD、SpotBugs）。仅构建服务端模块时仍可使用 `mvn -B -f agent-jingu3-cs-server/pom.xml clean verify`。

**对话与客户端**：可选 HTTP 头 `X-Jingu3-Client-Platform` 或 JSON 体 `clientPlatform`（请求头优先）；服务端在送大模型前会附加 UTC 时间与平台行，意图路由仍仅用原始 `message`。详见 [docs/v0.5/接口文档.md](docs/v0.5/接口文档.md) §4；可选原始句密文落库见同目录部署文档与 `jingu3.chat` / `jingu3.crypto` 配置。

**运维联调**：HTTP 头 `X-Request-Id` / `X-Trace-Id` 与 JSON 体 `requestId` / `traceId` 会写入 Log4j2 MDC（`%X{requestId}`、`%X{traceId}`），便于日志与网关对齐。