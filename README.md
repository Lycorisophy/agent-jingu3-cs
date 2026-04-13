一款 **C/S 架构**的智能体（Agent）应用框架，服务端负责核心推理与工具调度，客户端提供跨平台（Windows/macOS/Linux）交互界面并承担本地资源访问职责。

jingu3的名字取自紧箍咒/金箍棒/禁锢+三角形（具有稳定性）/不可能三角/三生万物/下面的三大

该框架核心三大系统：8大行动模式（包含意图场景失败）+多层记忆体系+技能系统

三大工程：提示词工程，上下文工程，驾驭工程

三大模型：嵌入模型、BERT类小模型、多模态MOE大模型

我就是要用互联网思维，企业级规范，JAVA生态来完成这一个大框架，目的就是开源

---

## 文档索引

| 文档 | 说明 |
|------|------|
| [docs/规范/jingu3-开发规范.md](docs/规范/jingu3-开发规范.md) | 史诗顺序、版本文档流水线、`docs/{版本号}/` 约定、Ollama 与单用户 |
| [docs/计划/开发路线图.md](docs/计划/开发路线图.md) | v0.1+ 版本目标、功能与文档工序勾选 |
| [docs/v0.1/README.md](docs/v0.1/README.md) | v0.1 文档清单（史诗① 启动已交付） |
| [docs/v0.3/README.md](docs/v0.3/README.md) | v0.3 文档清单（工具子系统 MVP + Ask/ReAct 真闭环） |
| [docs/服务架构.md](docs/服务架构.md) | 总体架构与技术选型 |

服务端入口：`agent-jingu3-cs-server/`，主类 `cn.lysoy.jingu3.Jingu3Application`；v0.1 提供 `POST /api/v1/chat` 与 Actuator 健康检查，详见 [docs/v0.1/接口文档.md](docs/v0.1/接口文档.md)。

**运维联调**：HTTP 头 `X-Request-Id` / `X-Trace-Id` 与 JSON 体 `requestId` / `traceId` 会写入 Log4j2 MDC（`%X{requestId}`、`%X{traceId}`），便于日志与网关对齐。