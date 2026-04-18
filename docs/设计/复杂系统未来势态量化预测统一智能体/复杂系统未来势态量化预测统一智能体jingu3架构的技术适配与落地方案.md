# **复杂系统未来势态量化预测统一智能体**
## —— 基于 Jingu3 C/S 架构的技术适配与落地方案

| 文档编号 | UAP-JINGU3-2026-001 |
| :--- | :--- |
| **版本** | 1.1 |
| **编制日期** | 2026年4月14日 |
| **前置依赖** | [服务架构.md](../../服务架构.md)（Jingu3 C/S 总体设计，修订见文内版本表） |
| **目标版本** | **v0.10～v0.12**（UAP 三连版本；**唯一排期以** [开发路线图.md](../../计划/开发路线图.md) **为准**） |
| **核心结论** | 现有 C/S 架构具备极好的可扩展性，通过扩展 **记忆层、工具层、引擎层** 即可平滑演进为复杂系统预测决策智能体。 |

---

## 摘要

本报告是 **复杂系统未来势态量化预测统一智能体（UAP）** 在 **Jingu3 C/S 架构** 中的具体落地执行方案。报告严格遵循 Jingu3 的技术选型（Spring Boot 3.x, LangChain4j, Milvus, MySQL, Electron），将 UAP 的六维核心能力（记忆-知识-搜索-工具-意图-行动）映射为具体的 Java 包结构、数据库表扩展、客户端功能模块及实施路线图。

方案核心是 **“引擎升级 + 工具扩展 + 记忆双写”**，即：不修改现有框架核心逻辑，而是通过新增 `cn.lysoy.jingu3.predict` 和 `cn.lysoy.jingu3.agent.advanced` 包，利用 LangChain4j 的扩展点，在现有 ReAct 循环中注入动力学推理能力。

---

## 目录

1. 架构融合与演进路径
2. 核心能力模块的 Jingu3 适配设计
    - 2.1 高级记忆系统：情节与语义双存储
    - 2.2 知识图谱集成：轻量级物理约束校验
    - 2.3 动力学工具集：Koopman/PINN/PESM 的 Java 封装
    - 2.4 联网搜索与事件同化
    - 2.5 意图场景识别与行动模式状态机
3. 数据模型扩展设计
4. 客户端适配：可视化与本地安全计算
5. 技术选型补充与集成细节
6. 分阶段实施路线图（对齐 Jingu3 现有里程碑）
7. 风险评估与应对策略
8. 结论

---

## 1. 架构融合与演进路径

### 1.1 融合原则

- **非侵入式扩展**：保留现有 `cn.lysoy.jingu3` 下的 `controller`, `engine`, `tool` 基础逻辑，通过 **新增模块** 和 **配置开关** 引入 UAP 特性。
- **LangChain4j 为核心编排器**：利用其 `AiServices`, `ToolSpecification` 动态注册动力学工具。
- **C/S 边界明确**：计算密集型任务（Koopman 分解、PINN 仿真）留在服务端；数据可视化、本地文件处理在客户端。

### 1.2 架构演进全景图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    客户端 (Electron + React) - Jingu3 UI                 │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  新增：态势感知仪表盘（Plotly.js 3D 相图、不确定性云图、反事实推演面板）  │   │
│  │  新增：本地 Python 侧车（可选，用于轻量级预处理）                       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │ WebSocket / REST (JWT)
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      Java 服务端 (Spring Boot) - Jingu3 Server           │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                         网关层 (不变)                               │ │
│  └───────────────────────────────┬────────────────────────────────────┘ │
│                                  ▼                                       │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │               智能体引擎 (Agent Engine) - **核心扩展区**              │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │ │
│  │  │ 记忆管理器    │  │ 意图路由器    │  │  行动模式状态机           │  │ │
│  │  │ (Episodic/Sem)│  │ (IntentRouter)│  │  (Auto/Advisory/Control) │  │ │
│  │  └──────┬───────┘  └──────┬───────┘  └───────────┬──────────────┘  │ │
│  │         │                 │                      │                  │ │
│  │         └─────────┬───────┴──────────┬───────────┘                  │ │
│  │                   ▼                  ▼                              │ │
│  │            ┌─────────────────────────────────┐                      │ │
│  │            │   增强型 ReAct 循环 (LangChain4j) │                      │ │
│  │            └─────────────────────────────────┘                      │ │
│  └───────────────────────────────┬────────────────────────────────────┘ │
│                                  ▼                                       │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │             工具调用调度器 (Tool Registry) - **工具扩展区**           │ │
│  │  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────────┐ │ │
│  │  │ 动力学工具        │ │ 联网搜索工具      │ │ 知识图谱查询工具      │ │ │
│  │  │ (Koopman/PINN)   │ │ (Tavily/Brave)   │ │ (Neo4j/规则引擎)      │ │ │
│  │  └──────────────────┘ └──────────────────┘ └──────────────────────┘ │ │
│  └───────────────────────────────┬────────────────────────────────────┘ │
│                                  ▼                                       │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                        数据层 - **存储扩展区**                       │ │
│  │  MySQL (新增表)    │  Milvus (情节记忆集合)  │  Neo4j (知识图谱)    │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 核心能力模块的 Jingu3 适配设计

### 2.1 高级记忆系统：情节与语义双存储

Jingu3 现有记忆基于 `CacheService`（短期）和 Milvus（长期语义检索）。为支持 UAP，需升级为 **情节记忆** 与 **语义记忆** 分离的结构。

#### 2.1.1 设计目标

| 记忆类型 | 现有能力 | 需扩展能力 | 存储方案 |
| :--- | :--- | :--- | :--- |
| **情节记忆** | 无（仅对话片段） | 存储 `(状态快照，决策，结果，反思)` 四元组 | **Milvus 新 Collection** `episodic_memory` |
| **语义记忆** | 无（仅文本相似） | 存储领域规则、公式、因果链 | **MySQL JSON 字段** 或 **轻量图库**（如 Apache Jena） |
| **工作记忆** | `CacheService` | 扩展为 `ConversationContext` 对象 | Redis / Caffeine |

#### 2.1.2 情节记忆存储结构（Milvus Collection 设计）

```json
{
  "collection_name": "episodic_memory",
  "fields": [
    {"name": "id", "type": "VARCHAR", "is_primary": true},
    {"name": "user_id", "type": "VARCHAR"},
    {"name": "domain", "type": "VARCHAR"},          // 如 "power_grid", "supply_chain"
    {"name": "state_snapshot", "type": "JSON"},     // 如 {"frequency":49.92, "rocof":0.12}
    {"name": "action_taken", "type": "JSON"},       // 工具调用记录
    {"name": "outcome_metrics", "type": "JSON"},    // 结果量化指标
    {"name": "reflection", "type": "VARCHAR"},      // LLM 生成的反思摘要
    {"name": "state_vector", "type": "FLOAT_VECTOR", "dim": 768} // Ollama 嵌入
  ]
}
```

**检索流程增强**：
1. 用户输入 + 当前系统状态 → 调用 Ollama Embedding 生成 `query_vector`。
2. 在 `episodic_memory` 中搜索 Top-K，过滤 `user_id` 和 `domain`。
3. 将检索到的 `reflection` 和 `action_taken` 注入 LangChain4j 的 `PromptTemplate`。

#### 2.1.3 代码实现思路

```java
// 包路径：cn.lysoy.jingu3.memory.episodic
@Service
public class EpisodicMemoryService {
    private final MilvusClient milvusClient;
    private final OllamaEmbeddingClient embeddingClient;
    
    public List<EpisodicMemoryEntry> retrieveSimilarEpisodes(String domain, Map<String, Object> currentState) {
        String stateJson = objectMapper.writeValueAsString(currentState);
        List<Float> vector = embeddingClient.embed(stateJson);
        // Milvus 搜索逻辑...
    }
    
    public void saveEpisode(EpisodicMemoryEntry entry) { ... }
}
```

### 2.2 知识图谱集成：轻量级物理约束校验

复杂系统预测需要硬约束（如“电网频率不得低于 49.5Hz”）。考虑到运维成本，**不强制引入 Neo4j**，采用 **MySQL + 规则引擎** 的轻量方案。

#### 2.2.1 方案对比与选型

| 方案 | 优势 | 劣势 | 适用阶段 |
| :--- | :--- | :--- | :--- |
| **MySQL JSON 字段** | 零额外依赖，开发快 | 图查询复杂时性能差 | **Phase 1-3 首选** |
| **Apache Jena (内置)** | Java 原生，支持 SPARQL 推理 | 内存占用稍高 | Phase 3+ 引入 |
| **Neo4j** | 专业图数据库，可视化强 | 增加运维复杂度 | 生产环境可选增强 |

#### 2.2.2 MySQL 表设计（领域知识库）

```sql
CREATE TABLE `domain_knowledge` (
    `id` CHAR(36) PRIMARY KEY,
    `domain` VARCHAR(50) NOT NULL COMMENT '领域标识',
    `entity_type` VARCHAR(50) COMMENT 'Node/Edge/Rule',
    `name` VARCHAR(255) NOT NULL,
    `properties` JSON COMMENT '属性/公式/约束',
    `relations` JSON COMMENT '关系：{"causes":"frequency_drop", "effect":"load_shedding"}',
    INDEX `idx_domain_entity` (`domain`, `entity_type`)
);
```

**示例数据**：
```json
{
  "domain": "power_grid",
  "entity_type": "Rule",
  "name": "低频减载触发条件",
  "properties": {
    "condition": "frequency < 49.5",
    "action": "trigger_load_shedding",
    "priority": 1
  }
}
```

### 2.3 动力学工具集：Koopman/PINN/PESM 的 Java 封装

这是 UAP 的核心计算模块。Jingu3 的 `Jingu3Tool` 接口设计非常利于扩展此类工具。

#### 2.3.1 工具接口适配

所有动力学工具实现 `Jingu3Tool`，并通过 `@Component` 自动注册。

```java
@Component
public class KoopmanPredictTool implements Jingu3Tool {
    @Override
    public String id() { return "predict_koopman"; }
    
    @Override
    public String description() { 
        return "使用库普曼算子对时序数据进行轨迹外推。输入参数：data(二维数组JSON), horizon(步长)。返回预测轨迹和置信区间。"; 
    }
    
    @Override
    public String execute(String input) throws ToolExecutionException {
        // 1. 解析 JSON 输入
        // 2. 调用 Python 微服务或本地 JNI/ProcessBuilder 执行 PyTorch 脚本
        // 3. 返回 JSON 结果
    }
}
```

#### 2.3.2 Python 算法侧车部署

由于 Koopman/PINN 依赖 PyTorch 生态，建议采用 **独立 Python 微服务** 或 **本地子进程调用**。

**推荐方案**：
- **开发/测试**：`ProcessBuilder` 调用本地 Python 脚本（需用户安装 Python 环境）。
- **生产**：部署 Python 微服务（FastAPI），Java 通过 HTTP 调用。

**接口协议**：
```json
POST /predict/koopman
{
  "data": [[t1, v1], [t2, v2], ...],
  "horizon": 10
}
Response:
{
  "trajectory": [...],
  "confidence_interval": {"lower": [...], "upper": [...]}
}
```

### 2.4 联网搜索与事件同化

扩展现有工具集，添加 `WebSearchTool` 实现 `Jingu3Tool`。利用 Tavily 或 Brave Search API。

```java
@Component
public class WebSearchTool implements Jingu3Tool {
    private final RestTemplate restTemplate;
    
    @Override
    public String execute(String query) {
        // 调用 Tavily API，返回 JSON 摘要
        // 内部可调用 LLM 进行摘要提取，转化为扰动标签
    }
}
```

### 2.5 意图场景识别与行动模式状态机

#### 2.5.1 意图识别路由器

在 `AgentEngine` 中增加 `IntentRouter`，利用 **LangChain4j 的 AiServices 带枚举返回** 功能。

```java
interface IntentClassifier {
    @UserMessage("分析用户意图：{{input}}。可选意图：SAFETY_CRITICAL, PROFIT_SEEKING, INFORMATIONAL, RISK_MANAGEMENT")
    Intent classify(String input);
}
```

#### 2.5.2 行动模式状态机实现

新增包 `cn.lysoy.jingu3.agent.mode`，管理全局模式。

```java
@Component
public class ActionModeStateMachine {
    private Mode currentMode = Mode.AUTO_PILOT;
    
    public Mode evaluateTransition(EntropyLevel entropy, double predictionVariance) {
        if (entropy == EntropyLevel.UNKNOWN_VOID) {
            return Mode.TAKE_CONTROL;
        }
        if (entropy == EntropyLevel.TURBULENT || predictionVariance > threshold) {
            return Mode.ADVISORY;
        }
        return Mode.AUTO_PILOT;
    }
}
```

---

## 3. 数据模型扩展设计

在现有 `messages` 表基础上增加字段，并新建预测任务相关表。

```sql
-- 扩展现有 messages 表
ALTER TABLE messages ADD COLUMN `intent` VARCHAR(30) COMMENT '意图标签';
ALTER TABLE messages ADD COLUMN `uncertainty_level` VARCHAR(30) COMMENT '不确定性等级';

-- 新建预测任务表
CREATE TABLE `prediction_tasks` (
    `id` CHAR(36) PRIMARY KEY,
    `conversation_id` CHAR(36) NOT NULL,
    `domain` VARCHAR(50) NOT NULL,
    `input_state` JSON NOT NULL COMMENT '输入状态快照',
    `predicted_trajectory` JSON COMMENT '预测轨迹',
    `confidence_interval` JSON COMMENT '置信区间',
    `entropy_score` FLOAT COMMENT '排列熵/MWPE',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4. 客户端适配：可视化与本地安全计算

### 4.1 态势感知仪表盘

基于现有 React 客户端，新增 `PredictiveDashboard` 组件。
- **图表库**：Plotly.js（支持 3D 相空间重构展示）。
- **实时数据流**：通过 WebSocket 接收 `type: "prediction_chunk"` 消息，动态更新曲线。

### 4.2 本地 Python 侧车（可选）

若用户对数据隐私要求极高，可利用 Electron 的 Node.js 能力调用本地 Python 环境执行轻量级预测（如 PESM 熵计算），结果仅回传摘要。

---

## 5. 技术选型补充与集成细节

| 组件 | 原 Jingu3 选型 | UAP 增强选型 | 集成方式 |
| :--- | :--- | :--- | :--- |
| **LLM 编排** | LangChain4j | LangChain4j (不变) | 利用 `ToolSpecification` 动态注册动力学工具 |
| **嵌入模型** | Ollama Embedding | Ollama Embedding (不变) | 用于情节记忆向量化 |
| **向量库** | Milvus | Milvus (新增 Collection) | 通过 `MilvusServiceClient` 管理 |
| **图存储** | 无 | MySQL JSON / Apache Jena | Spring Data JPA |
| **算法后端** | 无 | Python FastAPI 微服务 | Spring Cloud OpenFeign / RestTemplate |
| **消息队列** | RocketMQ | RocketMQ (不变) | 异步处理预测任务 |

---

## 6. 分阶段实施路线图（对齐 [开发路线图](../../计划/开发路线图.md)）

下表为 **UAP 在仓库中的正式版本切分**；**旧稿**中将 Phase 1～4 对齐 v0.7 / v0.8 / v0.9 / v1.0 的映射 **已废止**，勿与下表并行使用。

| Jingu3 版本 | 主题 | 对齐本文章节 | 交付物（摘要） | 验收标准（摘要） |
| :--- | :--- | :--- | :--- | :--- |
| **v0.10** | 工具与侧车 | §2.3、§2.4、§5 | `Jingu3Tool` 动力学工具；Python/FastAPI 侧车契约；可选联网搜索；可选 RocketMQ | 仿真场景下工具调用成功率达标（见 `docs/v0.10/PRD.md`） |
| **v0.11** | 记忆与领域知识 | §2.1、§2.2、§3 | 情节记忆 Milvus 策略；`domain_knowledge`；与现有记忆管线协同 | 检索延迟等指标见 `docs/v0.11/PRD.md` |
| **v0.12** | 意图策略与闭环 | §2.5、§4 | 内部策略状态机；`prediction_tasks`；客户端仪表盘 MVP；可选 `jingu3-predict-core` | UAP MVP 端到端可演示（见 `docs/v0.12/PRD.md`） |

**前置**：通用框架 **v0.7** 技能与工作空间基线稳定（**不要求**用户系统 **v1.0**）；**v0.9** 为横切增强（非 UAP 主线），见路线图 **v0.9** 节。

**版本级验收**：以各目录 **`docs/v0.10`～`docs/v0.12` 的 PRD** 为唯一验收口径（待补齐）。

---

## 7. 风险评估与应对策略

| 风险 | 应对措施 |
| :--- | :--- |
| **Python 微服务调用延迟高** | 将常用算子（如排列熵）用 Java 重写；或使用 GraalVM Python 运行时。 |
| **Milvus 向量维度不匹配** | 配置中心动态获取 Ollama 模型维度，自动创建 Collection。 |
| **LangChain4j 版本迭代** | 锁定稳定版本，在 `pom.xml` 中明确版本号，升级前充分测试。 |

---

## 8. 结论

**Jingu3 C/S 架构是承载复杂系统预测智能体的理想基座**。其模块化设计、清晰的工具接口和现代化的技术栈，使得 UAP 的六维能力可以 **低摩擦、渐进式** 地集成。

本方案提供了从代码包结构、数据库表到实施路线的全链路设计。建议在 **v0.7** 基线稳定后，按路线图启动 **v0.10**（UAP 工具与侧车）的开发验证（**先于**用户系统 **v1.0**）；详细里程碑以 [开发路线图.md](../../计划/开发路线图.md) **v0.10～v0.12** 节为准。

---

*报告编制：Matrix Agent*  
*复核：Jingu3 架构组*  
*日期：2026年4月14日*