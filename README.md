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
| [`agent-jingu3-cs-client/`](agent-jingu3-cs-client/) | **Electron + Vue3 + Vite + TypeScript** 桌面客户端（`POST /api/v1/chat` 与 SSE 流式）；与 Maven **不**耦合，独立 `npm ci` / `npm run build` |
| [`agent-jingu3-cs-workflow-ui/`](agent-jingu3-cs-workflow-ui/) | **独立 BPMN 管理前端**（Vue3 + Vite + bpmn-js），调用 `GET/POST /api/v1/bpmn/**`（Flowable）；与 Maven **不**耦合；**与对话 JSON 工作流（`workflowId`）并存** |
| [`docs/`](docs/) | 规范、设计、各版本 `docs/v0.x/`、Flyway 源脚本 [`docs/data/migration/`](docs/data/migration) |
| [`.github/workflows/`](.github/workflows/) | CI：根目录 `mvn clean verify`；并行 `agent-jingu3-cs-client` 与 `agent-jingu3-cs-workflow-ui` 的 `npm ci` + `npm run build` |
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
| [docs/v1.0/README.md](docs/v1.0/README.md) | v1.0 文档清单（用户系统；**排期在 v0.12 之后**，原 v0.8 顺延） |
| [docs/v0.9/README.md](docs/v0.9/README.md) | v0.9 框架横切增强（非 UAP 主线） |
| [docs/v0.10/README.md](docs/v0.10/README.md) | v0.10 UAP：工具与侧车 |
| [docs/v0.11/README.md](docs/v0.11/README.md) | v0.11 UAP：记忆与领域知识 |
| [docs/v0.12/README.md](docs/v0.12/README.md) | v0.12 UAP：意图策略与产品闭环 |
| [docs/设计/复杂系统未来势态量化预测统一智能体/复杂系统未来势态量化预测统一智能体jingu3架构的技术适配与落地方案.md](docs/设计/复杂系统未来势态量化预测统一智能体/复杂系统未来势态量化预测统一智能体jingu3架构的技术适配与落地方案.md) | UAP 技术适配与落地方案（v0.10～v0.12 设计索引） |

服务端入口：`agent-jingu3-cs-server/`，主类 `cn.lysoy.jingu3.Jingu3Application`。核心对话为 `POST /api/v1/chat`（SSE/WebSocket 流式见实现与客户端规范）；另含 HITL、对话状态 DST、Cron、健康检查等 **`/api/v1/...`** 能力。**接口以各版本 `docs/v0.x/接口文档.md`、路线图勾选及** [docs/v0.5/openapi.yaml](docs/v0.5/openapi.yaml) **等为补充**；v0.1 契约说明见 [docs/v0.1/接口文档.md](docs/v0.1/接口文档.md)。

**本地构建与门禁**（与 [CI](.github/workflows/ci.yml) 一致）：在仓库根执行 `mvn -B clean verify`（Reactor 构建 `agent-jingu3-cs-server`，含单测、PMD、SpotBugs）。仅构建服务端模块时仍可使用 `mvn -B -f agent-jingu3-cs-server/pom.xml clean verify`。

**Flowable BPMN 与 JSON 工作流**：服务端嵌入 Flowable 7，`/api/v1/bpmn/**` 用于部署 BPMN、启动实例等；对话模式 `mode=WORKFLOW` 仍使用 classpath `workflows/*.json`（`workflowId`），**两条路径独立**。开发期该 REST **无鉴权**，匿名可部署/启停流程，**仅限内网或本地**；详见 [docs/workflow/workflow-design.md](docs/workflow/workflow-design.md) 与 [agent-jingu3-cs-workflow-ui/README.md](agent-jingu3-cs-workflow-ui/README.md)。

**独立工作流 UI（可选）**：进入 [`agent-jingu3-cs-workflow-ui/`](agent-jingu3-cs-workflow-ui/)，配置 `.env.development` 中 `VITE_API_BASE=http://localhost:8080`，执行 `npm ci` 与 `npm run dev`（默认 `http://localhost:5173`）。

**客户端（Electron）**：进入 [`agent-jingu3-cs-client/`](agent-jingu3-cs-client/)，复制 [`.env.example`](agent-jingu3-cs-client/.env.example) 为 `.env`（可选）；安装与构建：

```bash
cd agent-jingu3-cs-client
npm ci
npm run build
npm run dev
```

开发期渲染页走 Vite 同源代理 `/api` → 后端（默认 `http://127.0.0.1:8080`，可用环境变量 `VITE_DEV_API_PROXY` 覆盖，见 `electron.vite.config.ts`）。打包后请在 `.env` 或构建参数中设置 `VITE_API_BASE` 指向后端根地址。若本机安装 Electron 时 TLS 校验失败，可尝试 `NODE_OPTIONS=--use-system-ca npm ci`（以企业根证书环境为准）。

**对话与客户端**：可选 HTTP 头 `X-Jingu3-Client-Platform` 或 JSON 体 `clientPlatform`（请求头优先）；服务端在送大模型前会附加 UTC 时间与平台行，意图路由仍仅用原始 `message`。详见 [docs/v0.5/接口文档.md](docs/v0.5/接口文档.md) §4；可选原始句密文落库见同目录部署文档与 `jingu3.chat` / `jingu3.crypto` 配置。

**运维联调**：HTTP 头 `X-Request-Id` / `X-Trace-Id` 与 JSON 体 `requestId` / `traceId` 会写入 Log4j2 MDC（`%X{requestId}`、`%X{traceId}`），便于日志与网关对齐。