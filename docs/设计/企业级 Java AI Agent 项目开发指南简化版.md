# 企业级 Java AI Agent 项目开发指南简化版 v0.1

本指南基于 **LangChain4j** 与 **LangGraph4j** 框架，整合多轮讨论的架构设计、生产级实践与踩坑经验，为企业级 AI Agent 项目提供可落地的开发参考。

---

## 一、核心架构总览

```text
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (渐进式披露UI)                        │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                      API 网关 / 会话管理                          │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                    IntelligentAgentService                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │意图/场景识别 │→│ 动态模型路由 │→│ 执行模式决策 (ReAct/Plan)│ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
└─────────────────────────────┬───────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌────────────────┐    ┌───────────────┐
│  ReAct Agent  │    │Plan-Execute    │    │  RAG 检索     │
│ (AgentExecutor)│    │   Graph        │    │ (Milvus)      │
└───────────────┘    └────────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              工具层 (系统工具 + 用户自定义 + Agent动态生成)         │
│         动态ToolProvider · 沙箱执行 · 版本化工具描述               │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                    状态持久化与上下文管理                          │
│   ┌──────────────────┐        ┌──────────────────────────────┐  │
│   │ Checkpointer     │        │ 双表设计                     │  │
│   │ (PG / Redis)     │        │ A表: user_conversation_history│  │
│   │ 存储 State 快照   │        │ B表: langgraph_checkpoints   │  │
│   └──────────────────┘        └──────────────────────────────┘  │
│                   上下文压缩 (Summarizer LLM)                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、技术栈选型

| 组件 | 推荐方案 | 说明 |
|:---|:---|:---|
| **AI 编排框架** | LangGraph4j | 状态机驱动的多 Agent 工作流 |
| **LLM 调用封装** | LangChain4j | 统一的模型接口、工具注解、记忆管理 |
| **向量数据库** | Milvus | 高性能向量检索，支持 HNSW 索引 |
| **关系数据库** | PostgreSQL | 存储用户历史、检查点、工具定义 |
| **缓存** | Redis | 热状态缓存、会话临时数据 |
| **配置中心** | Nacos / Spring Cloud Config | 动态刷新模型配置、提示词版本 |
| **模型 Provider** | OpenAI / Azure OpenAI / Ollama | 多模型动态切换 |

---

## 三、关键模块设计

### 3.1 动态模型管理与路由

**配置属性（支持热刷新）**：

```yaml
ai:
  models:
    instances:
      default:
        provider: openai
        api-key: ${OPENAI_API_KEY}
        model-name: gpt-4
      fast:
        provider: openai
        model-name: gpt-3.5-turbo
```

**模型工厂**：运行时按需创建并缓存实例，通过 `@RefreshScope` + 事件监听实现配置热刷新。

**模型路由器**：根据意图/场景复杂度动态选择模型：

```java
public ChatLanguageModel routeByScene(Scene scene, Intent intent) {
    if (scene == Scene.COMPLEX || intent == Intent.RESEARCH) {
        return factory.getModel("default");
    }
    return factory.getModel("fast");
}
```

### 3.2 意图识别与搜索决策（LLM 动态输出）

**核心思路**：让 LLM 一次性输出包含意图、场景、搜索决策、搜索关键词的结构化 JSON。

```java
@UserMessage("""
    分析用户输入，返回 JSON：
    {
      "intent": "RESEARCH",
      "scene": "COMPLEX",
      "needMemorySearch": true,
      "needWebSearch": true,
      "searchQueries": ["关键词1", "关键词2"]
    }
    """)
String classify(String userInput);
```

**解析步骤**：在 `IntentSceneService` 中使用 `ObjectMapper` 将 LLM 返回的字符串强类型化为 `ClassificationResult`，后续流程严格依据其中的 `needMemorySearch`、`needWebSearch`、`searchQueries` 执行。

### 3.3 执行模式选择

| 模式 | 适用场景 | LangGraph4j 实现 |
|:---|:---|:---|
| **ReAct** | 需要灵活工具调用、动态调整的任务 | `AgentExecutor.builder().chatModel(model).toolsFromObject(tools).build()` |
| **Plan-and-Execute** | 需要全局规划、多步骤拆解的复杂任务 | 自定义 StateGraph：`parse → exec_step → finalize` 循环 |

### 3.4 RAG 检索（Milvus 集成）

- 使用 `MilvusEmbeddingStore` 作为向量存储。
- **HNSW 索引**：RAG 场景首选，毫秒级查询延迟。
- 文档摄入管道：`Document → Splitter → Embedding → Milvus`。

### 3.5 工具系统（支持渐进式披露与动态扩展）

**`@Tool` 注解关键参数**：

| 参数 | 作用 |
|:---|:---|
| `value` | **工具描述**，直接影响 LLM 调用决策，应包含适用场景、限制条件 |
| `name` | 工具唯一标识 |
| `returnBehavior` | `TO_LLM`（默认）或 `IMMEDIATE` |

**场景约束实现**：
- **运行时判断**：工具方法内部检查 `ExecutionContext.getMode()`，返回友好错误提示。
- **条件装配**：使用 Spring `@Conditional` 根据操作系统等环境决定是否加载 Bean。
- **动态 ToolProvider**：根据当前会话的模式、用户权限等动态返回可用工具列表。

**渐进式披露**：
- 默认只暴露工具的 `name` + 简短 `description`（元数据层）。
- 用户表达明确意图后，再暴露完整参数 Schema（核心指令层）。
- 复杂文档、代码示例等作为参考资源层，按需读取。

### 3.6 上下文管理与对话持久化

**双表设计**：

| 表名 | 用途 | 存储内容 |
|:---|:---|:---|
| `langgraph_checkpoints` | **LLM 看的“B 表”** | 完整的 `State` 快照（含 `messages`、工具调用中间结果） |
| `user_conversation_history` | **用户看的“A 表”** | 过滤后的纯对话记录（仅 User/Assistant 消息） |

**Checkpoint 机制**：
- 每次节点执行后，LangGraph 自动将 `State` 序列化存入 `langgraph_checkpoints`。
- 通过 `RunnableConfig.threadId` 关联会话，用户登录后自动恢复上下文。

**上下文压缩（防爆仓）**：
- **触发时机**：`messages` 总 Token 数超过阈值（如模型上限的 80%）。
- **压缩方式**：启动一个轻量 Summarizer LLM，将早期对话压缩为摘要，替换原始消息。
- **用户无感**：压缩在等待首个响应 Token 的间隙异步执行，或在下一次请求前预先完成。

### 3.7 流式输出与交互反馈

`CompiledGraph.stream()` 返回的是**状态快照流**，每次节点执行完毕推送一次事件：

| 事件 | 前端展示 |
|:---|:---|
| `AiMessage` 包含 `ToolExecutionRequest` | 显示“正在调用工具：xxx” |
| `ToolExecutionResultMessage` | 显示工具执行结果（可折叠） |
| `AiMessage` 纯文本 | 逐字显示最终回答 |

如需 Token 级流式，需在节点内订阅 `StreamingChatLanguageModel` 的回调，并通过自定义事件通道推送。

---

## 四、生产级增强实践

### 4.1 版本化提示词与工具

**问题**：系统提示词或工具更新后，历史会话中保存的旧版本引用会导致行为不一致。

**方案**：在 `State` 中仅存储**版本号**，运行时从配置中心拉取对应版本的内容。

```java
// State 中存储
String systemPromptVersion = "v1.2.3";

// 运行时恢复
String prompt = promptRegistry.getVersion(systemPromptVersion);
```

### 4.2 数据生命周期管理

| 数据类型 | TTL 策略 |
|:---|:---|
| `langgraph_checkpoints` | 保留最近 3 个快照 或 24 小时自动清理 |
| `user_conversation_history` | 永久保留（可归档至冷存储） |
| Redis 会话缓存 | 30 分钟无操作自动过期 |

### 4.3 安全与隔离

- **用户工具沙箱**：使用 GraalJS 或 WebAssembly 执行用户自定义代码，严格限制网络/文件访问。
- **HTTP 白名单**：用户定义的 HTTP 工具仅允许访问预授权域名。
- **权限校验**：工具执行前验证当前用户是否有调用权限。

### 4.4 可观测性

- **指标监控**：单次请求 Token 用量、工具调用耗时、Checkpoint 读写延迟。
- **链路追踪**：在 LangGraph 节点前后埋点，关联 `thread_id` 全链路日志。
- **告警规则**：上下文压缩频率过高、Checkpoint 恢复失败率上升。

---

## 五、开发路线图建议

| 阶段 | 目标 | 关键交付物 |
|:---|:---|:---|
| **Phase 1: 基础框架** | 跑通 ReAct Agent，支持工具调用 | 模型动态配置、基础工具集、流式输出 |
| **Phase 2: 上下文管理** | 实现 Checkpoint 持久化与对话恢复 | PostgreSQL 集成、双表设计、压缩策略 |
| **Phase 3: RAG 集成** | 接入向量数据库，支持私有知识问答 | Milvus 部署、文档摄入管道、混合检索 |
| **Phase 4: 多模式编排** | 支持 Plan-and-Execute 模式 | 自定义 StateGraph、模式路由决策 |
| **Phase 5: 生产增强** | 用户自定义工具、版本管理、监控 | 工具构建器 UI、提示词注册中心、Prometheus 指标 |

---

## 六、常见问题速查

| 问题 | 解决方案 |
|:---|:---|
| 模型配置写死，更换 API Key 需重启 | 使用 `@ConfigurationProperties` + `@RefreshScope` + 工厂缓存刷新 |
| 历史对话过长导致 Token 超限 | 实施滑动窗口裁剪 + 摘要压缩，异步执行 |
| 工具在 Ask 模式下误调用修改文件 | 工具内部检查 `ExecutionContext.getMode()` 并返回错误提示 |
| 用户登录后丢失上下文 | 前端传入 `thread_id`，后端通过 Checkpointer 自动恢复 |
| 流式输出时看不到“思考”过程 | 根据 `StreamingOutput` 中的消息类型推送不同状态事件 |

---

## 七、参考资料

- LangGraph4j 官方文档：[https://github.com/bsorrentino/langgraph4j](https://github.com/bsorrentino/langgraph4j)
- LangChain4j 官方文档：[https://docs.langchain4j.dev](https://docs.langchain4j.dev)
- Milvus 向量数据库：[https://milvus.io/docs](https://milvus.io/docs)

---

> **版本记录**：v0.1 - 初始版本，涵盖架构设计、核心模块实现、生产级增强实践。