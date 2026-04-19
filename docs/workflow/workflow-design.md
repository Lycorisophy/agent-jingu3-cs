# JinGu3 AI Agent 工作流模式设计方案

## 〇、本仓库当前落地（MVP）

以下内容将**已实现能力**与下文「愿景/扩展」区分，避免与对话侧 JSON 工作流混淆。

| 维度 | 对话 JSON 工作流 | Flowable BPMN |
|------|------------------|---------------|
| 入口 | `POST /api/v1/chat`，`mode=WORKFLOW`，`workflowId` | `POST /api/v1/bpmn/**`（部署、启动实例等） |
| 定义来源 | classpath `workflows/*.json`，`WorkflowModeHandler` | BPMN 2.0 XML，引擎表 `ACT_*`，与 Spring **共用同一 DataSource** |
| 独立前端 | 无（走 Electron 客户端对话） | [`agent-jingu3-cs-workflow-ui/`](../../agent-jingu3-cs-workflow-ui/)（Vite + Vue3 + bpmn-js） |
| 产品参考 | — | 编排体验可对齐 [扣子工作流指南](https://www.coze.cn/open/docs/guides/workflow) 的心智（**非** Coze 协议或云端 API 对接） |

**安全**：MVP 下 `/api/v1/bpmn/**` **无登录鉴权**，匿名可部署与启停流程，**仅适用于内网/开发**；上线前必须收敛认证与权限。示例 BPMN 见服务端 [`agent-jingu3-cs-server/src/main/resources/processes/demo-llm.bpmn20.xml`](../../agent-jingu3-cs-server/src/main/resources/processes/demo-llm.bpmn20.xml)，Delegate Bean 名为 `jingu3LlmDelegate`。

---

## 一、项目背景与目标

### 1.1 项目概述
JinGu3 AI Agent 是一个智能助手平台，已实现安全对话、思考行动、计划行动、多智能体等多种模式。现需要新增**工作流模式**，让用户能够创建、分享和使用固定业务流程模板。

### 1.2 设计目标
1. 支持可视化工作流设计器（BPMN 2.0标准）
2. 实现工作流广场，支持模板分享与发现
3. 与现有AI Agent模式无缝集成
4. 提供完整的流程执行、监控与管理能力

---

## 二、技术选型

### 2.1 工作流引擎选型

| 维度 | Flowable 7.x | Camunda | Activiti |
|------|--------------|---------|----------|
| Spring Boot 3.x支持 | ✅ 完美支持 | ✅ 支持 | ❌ 仅支持到2.x |
| BPMN 2.0支持 | ✅ 完整 | ✅ 完整 | ✅ 完整 |
| AI Agent集成 | ✅ 官方推出AI Studio | ✅ 支持 | ⚠️ 需自行集成 |
| 前端生态(bpmn-js) | ✅ 完善 | ✅ 完善 | ✅ 完善 |
| 社区活跃度 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| 学习曲线 | 中等 | 中等 | 较简单 |
| 商业授权 | 开源(Apache 2.0) | 开源(Apache 2.0) | 开源(Apache 2.0) |

**推荐选择：Flowable 7.x**
- 原生支持Spring Boot 3.x
- 官方已推出Flowable AI Studio
- 表结构设计完善，便于扩展
- 与本项目技术栈高度匹配

### 2.2 前端技术选型

| 组件 | 选型 | 说明 |
|------|------|------|
| 流程设计器 | bpmn-js + bpmn-js-properties-panel | 业界标准，Vue/React均支持 |
| 流程图渲染 | bpmn-js | 支持SVG导出 |
| UI框架 | Vue 3 + Element Plus / Ant Design Vue | 与现有前端保持一致 |
| 状态管理 | Pinia | Vue 3推荐 |

### 2.3 数据库选型

| 数据类型 | 推荐数据库 | 说明 |
|----------|-----------|------|
| 流程定义、部署信息 | MySQL | Flowable原生支持，ACID保证 |
| 流程实例运行时数据 | MySQL | 高并发写入，事务安全 |
| 流程历史数据 | MongoDB | 高并发查询，灵活扩展 |
| 流程附件、文件 | MinIO | S3兼容，低成本存储 |
| 缓存(流程定义) | Redis | 热点数据加速 |

---

## 三、整体架构设计

### 3.1 系统架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           前端 (Vue 3)                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ 聊天界面  │  │工作流设计器│  │工作流广场 │  │流程监控   │  │任务中心  │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  │
│       │             │             │             │             │        │
│       └─────────────┴─────────────┴─────────────┴─────────────┘        │
│                                 │                                        │
└─────────────────────────────────┼──────────────────────────────────────┘
                                  │ HTTP/REST
┌─────────────────────────────────┼──────────────────────────────────────┐
│                          Spring Boot 3.x                                │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐           │
│  │  工作流Controller │  │  AI Agent服务   │  │  广场服务       │           │
│  └────────┬───────┘  └────────┬───────┘  └────────┬───────┘           │
│           │                   │                   │                   │
│  ┌────────┴───────────────────┴───────────────────┴───────┐           │
│  │                    工作流服务层                          │           │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │           │
│  │  │流程定义服务 │ │流程实例服务 │ │任务服务   │  │AI节点服务 │  │           │
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │           │
│  └───────────────────────────────────────────────────────────┘           │
│                                 │                                        │
└─────────────────────────────────┼──────────────────────────────────────┘
                                  │
┌─────────────────────────────────┼──────────────────────────────────────┐
│                          数据存储层                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐                  │
│  │  MySQL   │  │ MongoDB  │  │  Redis   │  │  MinIO   │                  │
│  │(流程引擎) │  │(历史数据) │  │ (缓存)   │  │ (附件)   │                  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘                  │
└───────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Flowable表结构设计

#### 3.2.1 核心表分类

| 前缀 | 名称 | 用途 |
|------|------|------|
| ACT_RE_*| Repository | 流程定义、部署信息 |
| ACT_RU_* | Runtime | 运行时数据(流程实例、任务) |
| ACT_HI_* | History | 历史数据 |
| ACT_ID_* | Identity | 用户、组、权限 |
| ACT_GE_* | General | 通用数据(字节数组等) |

#### 3.2.2 扩展表设计

```sql
-- 工作流模板表(自定义扩展)
CREATE TABLE wf_template (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    tags VARCHAR(500),
    process_definition_id VARCHAR(64),
    icon VARCHAR(100),
    thumbnail LONGBLOB,
    usage_count INT DEFAULT 0,
    rating DECIMAL(3,2) DEFAULT 0,
    is_public BOOLEAN DEFAULT FALSE,
    is_official BOOLEAN DEFAULT FALSE,
    author_id VARCHAR(64),
    status VARCHAR(20) DEFAULT 'DRAFT',
    version INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_author (author_id),
    INDEX idx_is_public (is_public),
    INDEX idx_create_time (create_time)
);

-- 工作流模板版本表
CREATE TABLE wf_template_version (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    bpmn_xml LONGTEXT NOT NULL,
    svg_diagram LONGTEXT,
    change_log TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES wf_template(id)
);

-- 工作流分类表
CREATE TABLE wf_category (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id VARCHAR(64),
    icon VARCHAR(100),
    sort_order INT DEFAULT 0,
    is_system BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES wf_category(id)
);

-- AI节点配置表
CREATE TABLE wf_ai_node_config (
    id VARCHAR(64) PRIMARY KEY,
    node_id VARCHAR(100) NOT NULL,
    process_definition_id VARCHAR(64),
    model_provider VARCHAR(50),  -- openai/anthropic/local
    model_name VARCHAR(100),
    system_prompt TEXT,
    temperature DECIMAL(3,2) DEFAULT 0.7,
    max_tokens INT DEFAULT 2048,
    input_variables VARCHAR(1000),
    output_variable VARCHAR(100),
    retry_config JSON,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_node_id (node_id),
    INDEX idx_process_def (process_definition_id)
);

-- 流程实例与AI会话关联表
CREATE TABLE wf_ai_session (
    id VARCHAR(64) PRIMARY KEY,
    process_instance_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    current_node_id VARCHAR(100),
    context JSON,
    status VARCHAR(20) DEFAULT 'RUNNING',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_process_instance (process_instance_id),
    INDEX idx_session (session_id)
);
```

---

## 四、前端设计

### 4.1 工作流设计器页面

#### 布局结构
```
┌─────────────────────────────────────────────────────────────────────────┐
│  工具栏: [保存] [部署] [发布到广场] [导入] [导出] [撤销] [重做] [预览]   │
├────────────┬────────────────────────────────────────────┬───────────────┤
│            │                                            │   属性面板    │
│  节点面板   │              画布区域                      │               │
│            │                                            │  节点ID:      │
│  ▼ 开始节点 │    ┌────┐      ┌────┐      ┌────┐         │  节点名称:    │
│    开始     │    │开始│─────▶│LLM │─────▶│结束│         │               │
│  ▼ AI节点   │    └────┘      └────┘      └────┘         │  模型选择:    │
│    大模型   │                                            │  提示词:      │
│    知识检索 │                                            │  输入变量:    │
│    意图识别 │                                            │  输出变量:    │
│  ▼ 逻辑节点 │                                            │               │
│    条件分支 │                                            │               │
│    循环     │                                            │               │
│    结束     │                                            │               │
│  ▼ 工具节点 │                                            │               │
│    HTTP请求 │                                            │               │
│    代码执行 │                                            │               │
│    知识库   │                                            │               │
└────────────┴────────────────────────────────────────────┴───────────────┘
│                          迷你地图 / 缩放控制                             │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 核心功能
1. **拖拽式节点创建**：从左侧面板拖拽节点到画布
2. **连线管理**：节点间连线，支持条件连线
3. **属性配置**：右侧面板配置选中节点属性
4. **BPMN导入导出**：支持标准BPMN 2.0 XML
5. **SVG预览**：实时预览流程图效果
6. **版本管理**：保存多个版本，支持回滚

### 4.2 工作流广场页面

#### 布局结构
```
┌─────────────────────────────────────────────────────────────────────────┐
│  🔍 搜索模板...                    [筛选] [排序] [发布模板]              │
├─────────────┬───────────────────────────────────────────────────────────┤
│  分类导航    │                     模板卡片列表                           │
│             │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  ▼ 全部     │  │  📄 模板1   │  │  📄 模板2   │  │  📄 模板3   │         │
│  ▼ AI应用   │  │  智能客服   │  │  文章生成   │  │  数据分析   │         │
│    客服     │  │  ⭐4.8 1.2k │  │  ⭐4.5 800  │  │  ⭐4.9 2.1k │         │
│    问答     │  │  👤 张三    │  │  👤 李四    │  │  👤 官方    │         │
│  ▼ 办公     │  └─────────────┘  └─────────────┘  └─────────────┘         │
│    审批     │                                                           │
│    报告     │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  ▼ 数据处理 │  │  📄 模板4   │  │  📄 模板5   │  │  📄 模板6   │         │
│    ETL      │  │  翻译助手   │  │  简历筛选   │  │  营销文案   │         │
│    清洗     │  │  ⭐4.6 500  │  │  ⭐4.7 1.5k │  │  ⭐4.4 600  │         │
│             │  │  👤 王五    │  │  👤 赵六    │  │  👤 钱七    │         │
└─────────────┴───────────────────────────────────────────────────────────┘
```

#### 核心功能
1. **模板搜索**：支持关键词、分类、标签搜索
2. **模板筛选**：按分类、评分、使用量排序
3. **模板预览**：查看流程图、描述、使用说明
4. **一键使用**：基于模板创建工作流
5. **模板收藏**：收藏常用模板
6. **发布管理**：管理自己发布的模板

### 4.3 工作流运行监控页面

```
┌─────────────────────────────────────────────────────────────────────────┐
│  流程监控                                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│  统计卡片: [运行中: 128] [已完成: 1.2k] [异常: 3] [今日启动: 45]          │
├─────────────────────────────────────────────────────────────────────────┤
│  运行实例列表                                                            │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │ ID        │ 名称      │ 版本 │ 状态   │ 启动时间    │ 操作         │  │
│  ├───────────┼───────────┼──────┼────────┼─────────────┼──────────────┤  │
│  │ inst_001  │ 智能客服  │ v2   │ 运行中 │ 2026-04-13  │ [详情][取消]  │  │
│  │ inst_002  │ 文章生成  │ v1   │ 已完成 │ 2026-04-12  │ [详情]       │  │
│  └────────────────────────────────────────────────────────────────────┘  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │                     流程运行图                                      │  │
│  │    ┌────┐      ┌────┐      ┌────┐      ┌────┐                     │  │
│  │    │开始│─────▶│LLM │─────▶│分支│──┬──▶│结束│                     │  │
│  │    └────┘      └────┘      └────┘  │   └────┘                     │  │
│  │                                    │                               │  │
│  │                               ┌────┴────┐                          │  │
│  │                               │知识检索│                          │  │
│  │                               └─────────┘                          │  │
│  └────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、后端设计

### 5.1 模块结构

```
agent-jingu3-cs/
├── jingu3-workflow/                    # 工作流模块
│   ├── pom.xml
│   └── src/main/java/com/jingu3/workflow/
│       ├── WorkflowApplication.java
│       ├── config/
│       │   ├── FlowableConfig.java     # Flowable配置
│       │   └── MongoConfig.java        # MongoDB配置
│       ├── controller/
│       │   ├── ProcessDefinitionController.java
│       │   ├── ProcessInstanceController.java
│       │   ├── TaskController.java
│       │   ├── WorkflowTemplateController.java
│       │   └── WorkflowSquareController.java
│       ├── service/
│       │   ├── ProcessDefinitionService.java
│       │   ├── ProcessInstanceService.java
│       │   ├── TaskService.java
│       │   ├── TemplateService.java
│       │   ├── SquareService.java
│       │   └── AiNodeService.java      # AI节点服务
│       ├── handler/
│       │   ├── AiServiceTaskHandler.java   # AI服务任务处理器
│       │   ├── LLMCallActivityBehavior.java # LLM调用行为
│       │   └── WorkflowEventListener.java  # 流程事件监听
│       ├── model/
│       │   ├── entity/
│       │   │   ├── WorkflowTemplate.java
│       │   │   ├── WorkflowTemplateVersion.java
│       │   │   ├── WorkflowCategory.java
│       │   │   └── AiNodeConfig.java
│       │   ├── dto/
│       │   │   ├── TemplateCreateDTO.java
│       │   │   ├── TemplatePublishDTO.java
│       │   │   └── WorkflowStartDTO.java
│       │   └── vo/
│       │       ├── TemplateDetailVO.java
│       │       └── ProcessInstanceVO.java
│       ├── repository/
│       │   ├── WorkflowTemplateRepository.java
│       │   └── AiNodeConfigRepository.java
│       └── util/
│           ├── BpmnXmlUtils.java
│           └── FlowableUtils.java
```

### 5.2 核心服务接口

#### 5.2.1 流程定义服务

```java
public interface ProcessDefinitionService {
    /**
     * 部署流程定义
     */
    DeploymentResult deployProcess(String bpmnXml, String name, String category);

    /**
     * 获取流程定义列表
     */
    Page<ProcessDefinitionVO> listProcessDefinitions(ProcessDefinitionQuery query);

    /**
     * 获取流程定义详情
     */
    ProcessDefinitionVO getProcessDefinition(String processDefinitionId);

    /**
     * 获取流程图(BPMN XML)
     */
    String getProcessBpmnXml(String processDefinitionId);

    /**
     * 获取流程SVG图
     */
    String getProcessSvg(String processDefinitionId);

    /**
     * 挂起/激活流程定义
     */
    void updateProcessDefinitionState(String processDefinitionId, SuspensionState state);

    /**
     * 删除流程定义
     */
    void deleteProcessDefinition(String processDefinitionId, Integer cascade);
}
```

#### 5.2.2 模板服务

```java
public interface TemplateService {
    /**
     * 创建模板(草稿)
     */
    WorkflowTemplate createTemplate(TemplateCreateDTO dto);

    /**
     * 更新模板
     */
    void updateTemplate(String templateId, TemplateUpdateDTO dto);

    /**
     * 发布模板到广场
     */
    void publishTemplate(String templateId, TemplatePublishDTO dto);

    /**
     * 获取模板详情
     */
    TemplateDetailVO getTemplateDetail(String templateId);

    /**
     * 获取我的模板列表
     */
    Page<WorkflowTemplate> listMyTemplates(String userId, TemplateQuery query);

    /**
     * 删除模板
     */
    void deleteTemplate(String templateId);

    /**
     * 收藏/取消收藏模板
     */
    void toggleFavorite(String templateId, String userId);

    /**
     * 评分模板
     */
    void rateTemplate(String templateId, String userId, BigDecimal rating);
}
```

#### 5.2.3 广场服务

```java
public interface SquareService {
    /**
     * 搜索模板
     */
    Page<TemplateCardVO> searchTemplates(SquareSearchQuery query);

    /**
     * 获取热门模板
     */
    List<TemplateCardVO> getHotTemplates(int limit);

    /**
     * 获取最新模板
     */
    List<TemplateCardVO> getNewTemplates(int limit);

    /**
     * 获取推荐模板
     */
    List<TemplateCardVO> getRecommendedTemplates(String userId, int limit);

    /**
     * 获取分类模板
     */
    Page<TemplateCardVO> getTemplatesByCategory(String categoryId, PageQuery query);

    /**
     * 获取用户收藏
     */
    Page<WorkflowTemplate> getUserFavorites(String userId, PageQuery query);

    /**
     * 获取模板使用量统计
     */
    TemplateStatsVO getTemplateStats(String templateId);
}
```

### 5.3 AI节点服务(核心)

```java
@Service
public class AiNodeService {

    @Autowired
    private LlmClient llmClient;  // 大模型调用客户端

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    /**
     * 执行AI节点
     */
    public AiNodeResult executeAiNode(String nodeId, Map<String, Object> variables) {
        // 1. 获取节点配置
        AiNodeConfig config = getNodeConfig(nodeId);

        // 2. 解析输入变量
        Map<String, Object> inputs = resolveInputs(config.getInputVariables(), variables);

        // 3. 构建提示词
        String prompt = buildPrompt(config, inputs);

        // 4. 调用大模型
        LlmResponse response = callLlm(config, prompt);

        // 5. 解析输出
        Object output = parseOutput(config, response);

        return AiNodeResult.success(output);
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(AiNodeConfig config, Map<String, Object> inputs) {
        String template = config.getSystemPrompt();
        for (Map.Entry<String, Object> entry : inputs.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return template;
    }

    /**
     * 调用大模型
     */
    private LlmResponse callLlm(AiNodeConfig config, String prompt) {
        return llmClient.call(
            LlmRequest.builder()
                .provider(config.getModelProvider())
                .model(config.getModelName())
                .prompt(prompt)
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .build()
        );
    }

    /**
     * 知识检索节点
     */
    public KnowledgeSearchResult searchKnowledge(String nodeId, String query, int topK) {
        AiNodeConfig config = getNodeConfig(nodeId);
        String knowledgeBaseId = config.getKnowledgeBaseId();
        return knowledgeService.search(knowledgeBaseId, query, topK);
    }
}
```

### 5.4 自定义任务处理器

```java
@Component
public class AiServiceTaskHandler implements JavaDelegate {

    @Autowired
    private AiNodeService aiNodeService;

    @Override
    public void execute(DelegateExecution execution) {
        String nodeId = execution.getCurrentActivityId();
        Map<String, Object> variables = execution.getVariables();

        try {
            // 执行AI节点
            AiNodeResult result = aiNodeService.executeAiNode(nodeId, variables);

            // 设置输出变量
            execution.setVariable(result.getOutputVariable(), result.getOutput());

        } catch (Exception e) {
            // 记录错误日志
            log.error("AI节点执行失败: nodeId={}", nodeId, e);

            // 可以设置错误变量供后续网关判断
            execution.setVariable("ai_error", e.getMessage());
        }
    }
}
```

### 5.5 BPMN XML 示例

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://flowable.org/test">

    <!-- 开始事件 -->
    <startEvent id="startEvent1" name="开始"/>

    <!-- AI大模型任务 -->
    <serviceTask id="llmTask1" name="分析问题"
                 flowable:class="com.jingu3.workflow.handler.AiServiceTaskHandler"
                 flowable:nodeId="llmTask1"/>

    <!-- 条件分支 -->
    <exclusiveGateway id="decisionGateway" name="判断类型"/>

    <!-- 知识检索任务 -->
    <serviceTask id="knowledgeTask" name="检索知识库"
                 flowable:class="com.jingu3.workflow.handler.KnowledgeSearchTaskHandler"/>

    <!-- 结束事件 -->
    <endEvent id="endEvent1" name="正常结束"/>
    <endEvent id="endEvent2" name="异常结束"/>

    <!-- 流程连线 -->
    <sequenceFlow id="flow1" sourceRef="startEvent1" targetRef="llmTask1"/>
    <sequenceFlow id="flow2" sourceRef="llmTask1" targetRef="decisionGateway"/>

    <!-- 条件连线 -->
    <sequenceFlow id="flow3" sourceRef="decisionGateway" targetRef="knowledgeTask">
        <conditionExpression xsi:type="tFormalExpression">
            ${intent == 'search_knowledge'}
        </conditionExpression>
    </sequenceFlow>
    <sequenceFlow id="flow4" sourceRef="decisionGateway" targetRef="endEvent1">
        <conditionExpression xsi:type="tFormalExpression">
            ${intent == 'direct_answer'}
        </conditionExpression>
    </sequenceFlow>

    <sequenceFlow id="flow5" sourceRef="knowledgeTask" targetRef="endEvent1"/>
</definitions>
```

---

## 六、AI Agent与工作流集成

### 6.1 集成模式

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         模式选择逻辑                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   用户输入 ──▶ Auto模式 ──▶ AI判断 ──▶ 选择执行模式                      │
│                              │                                          │
│          ┌───────────────────┼───────────────────┐                      │
│          ▼                   ▼                   ▼                       │
│    ┌──────────┐       ┌──────────┐       ┌──────────┐                 │
│    │ 安全对话  │       │ 思考行动  │       │ 工作流模式 │                 │
│    └──────────┘       └──────────┘       └──────────┘                 │
│                                                  │                       │
│                                          ┌───────┴───────┐              │
│                                          ▼               ▼              │
│                                    ┌─────────┐     ┌─────────┐        │
│                                    │预定义流程│     │用户自建流程│        │
│                                    └─────────┘     └─────────┘        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 工作流模式执行流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         工作流执行流程                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. [启动] 用户选择工作流或AI推荐工作流                                   │
│                    │                                                    │
│                    ▼                                                    │
│  2. [初始化] 创建流程实例，传递输入参数                                  │
│                    │                                                    │
│                    ▼                                                    │
│  3. [执行节点] 遍历BPMN节点                                             │
│                    │                                                    │
│          ┌─────────┼─────────┐                                         │
│          ▼         ▼         ▼                                         │
│    ┌──────────┐┌──────────┐┌──────────┐                              │
│    │ 开始节点  ││AI任务节点││条件网关   │                              │
│    └────┬─────┘└────┬─────┘└────┬─────┘                              │
│         │           │           │                                      │
│         └───────────┴───────────┘                                      │
│                    │                                                    │
│                    ▼                                                    │
│  4. [AI节点执行]                                                       │
│    - LLM节点: 调用大模型，传递上下文                                    │
│    - 知识检索: 查询知识库                                               │
│    - 意图识别: 判断用户意图，决定路由                                    │
│                    │                                                    │
│                    ▼                                                    │
│  5. [状态更新] 记录执行状态到ACT_HI_*表                                 │
│                    │                                                    │
│                    ▼                                                    │
│  6. [完成/异常] 流程结束或异常处理                                      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.3 模式切换实现

```java
@Service
public class ModeRouter {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private AgentService agentService;

    /**
     * 路由到对应模式
     */
    public ModeResult route(String userInput, ModeType preferredMode) {
        if (preferredMode == ModeType.AUTO) {
            // AI自动判断
            return autoSelectMode(userInput);
        }

        return switch (preferredMode) {
            case SAFE_CHAT -> agentService.executeSafeChat(userInput);
            case THINK_ACTION -> agentService.executeThinkAction(userInput);
            case PLAN_ACTION -> agentService.executePlanAction(userInput);
            case WORKFLOW -> workflowService.executeWorkflow(userInput);
            case MULTI_AGENT -> agentService.executeMultiAgent(userInput);
            default -> throw new UnsupportedOperationException();
        };
    }

    /**
     * AI自动选择模式
     */
    private ModeResult autoSelectMode(String userInput) {
        // 调用LLM判断最适合的模式
        String prompt = String.format("""
            分析用户输入，判断最适合的执行模式：
            用户输入: %s

            模式说明:
            - safe_chat: 简单问答，无需工具调用
            - think_action: 单步思考+行动
            - plan_action: 多步规划后执行
            - workflow: 需要固定流程处理
            - multi_agent: 需要多智能体协作

            只返回模式名称，不要其他内容。
            """, userInput);

        String mode = llmClient.call(prompt).getContent().trim().toLowerCase();
        return route(userInput, ModeType.valueOf(mode));
    }
}
```

---

## 七、数据库详细设计

### 7.1 MySQL表结构(核心业务表)

```sql
-- ============================================
-- Flowable原生表前缀说明
-- ============================================
-- ACT_RE_* : Repository，存储流程定义
-- ACT_RU_* : Runtime，运行时数据
-- ACT_HI_* : History，历史数据
-- ACT_ID_* : Identity，身份数据
-- ACT_GE_* : General，通用数据

-- ============================================
-- 自定义业务表
-- ============================================

-- 1. 工作流模板表
CREATE TABLE wf_template (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键',
    name VARCHAR(255) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    category_id VARCHAR(64) COMMENT '分类ID',
    tags VARCHAR(500) COMMENT '标签，逗号分隔',
    icon VARCHAR(100) COMMENT '图标',
    thumbnail LONGBLOB COMMENT '缩略图',

    -- Flowable关联
    process_definition_key VARCHAR(100) COMMENT '流程定义Key',
    process_definition_id VARCHAR(64) COMMENT '流程定义ID',
    version INT DEFAULT 1 COMMENT '版本号',

    -- 统计信息
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    favorite_count INT DEFAULT 0 COMMENT '收藏次数',
    rating DECIMAL(3,2) DEFAULT 0 COMMENT '平均评分',
    rating_count INT DEFAULT 0 COMMENT '评分次数',

    -- 发布信息
    is_published BOOLEAN DEFAULT FALSE COMMENT '是否发布',
    is_public BOOLEAN DEFAULT FALSE COMMENT '是否公开',
    is_official BOOLEAN DEFAULT FALSE COMMENT '是否官方',

    -- 用户信息
    author_id VARCHAR(64) NOT NULL COMMENT '作者ID',
    author_name VARCHAR(100) COMMENT '作者名称',

    -- 状态
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT/PUBLISHED/ARCHIVED',

    -- 时间戳
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    publish_time TIMESTAMP NULL COMMENT '发布时间',

    INDEX idx_category (category_id),
    INDEX idx_author (author_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_usage_count (usage_count DESC)
) COMMENT '工作流模板表';

-- 2. 模板版本表
CREATE TABLE wf_template_version (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    bpmn_xml LONGTEXT NOT NULL COMMENT 'BPMN XML',
    svg_diagram LONGTEXT COMMENT 'SVG流程图',
    change_log TEXT COMMENT '变更日志',

    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_template_version (template_id, version),
    FOREIGN KEY (template_id) REFERENCES wf_template(id) ON DELETE CASCADE
) COMMENT '模板版本表';

-- 3. 工作流分类表
CREATE TABLE wf_category (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id VARCHAR(64) COMMENT '父分类ID',
    icon VARCHAR(100) COMMENT '图标',
    color VARCHAR(20) COMMENT '颜色',
    sort_order INT DEFAULT 0,
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统分类',

    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_parent (parent_id),
    INDEX idx_sort (sort_order)
) COMMENT '工作流分类表';

-- 4. AI节点配置表
CREATE TABLE wf_ai_node_config (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(100) NOT NULL COMMENT 'BPMN节点ID',
    node_name VARCHAR(255) COMMENT '节点名称',
    node_type VARCHAR(50) COMMENT '节点类型:llm/knowledge/intent',

    -- 模型配置
    model_provider VARCHAR(50) DEFAULT 'openai' COMMENT '模型提供商',
    model_name VARCHAR(100) COMMENT '模型名称',
    system_prompt TEXT COMMENT '系统提示词',
    temperature DECIMAL(3,2) DEFAULT 0.7,
    max_tokens INT DEFAULT 2048,
    top_p DECIMAL(3,2) DEFAULT 0.9,

    -- 变量配置
    input_variables VARCHAR(1000) COMMENT '输入变量，JSON格式',
    output_variable VARCHAR(100) COMMENT '输出变量名',

    -- 知识库配置
    knowledge_base_id VARCHAR(64) COMMENT '关联知识库ID',
    search_top_k INT DEFAULT 5,
    similarity_threshold DECIMAL(3,2) DEFAULT 0.7,

    -- 重试配置
    retry_count INT DEFAULT 3,
    retry_interval INT DEFAULT 1000,

    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_template (template_id),
    INDEX idx_node (node_id)
) COMMENT 'AI节点配置表';

-- 5. 流程实例与AI会话关联表
CREATE TABLE wf_ai_session (
    id VARCHAR(64) PRIMARY KEY,
    process_instance_id VARCHAR(64) NOT NULL,
    ai_session_id VARCHAR(64) COMMENT 'AI会话ID',

    -- 当前状态
    current_node_id VARCHAR(100) COMMENT '当前节点ID',
    current_node_name VARCHAR(255) COMMENT '当前节点名称',
    status VARCHAR(20) DEFAULT 'RUNNING' COMMENT 'RUNNING/COMPLETED/TERMINATED',

    -- 上下文
    context JSON COMMENT '上下文数据',

    -- 时间戳
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,

    INDEX idx_process_instance (process_instance_id),
    INDEX idx_ai_session (ai_session_id),
    INDEX idx_status (status)
) COMMENT '流程AI会话关联表';

-- 6. 模板收藏表
CREATE TABLE wf_template_favorite (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_template_user (template_id, user_id),
    FOREIGN KEY (template_id) REFERENCES wf_template(id) ON DELETE CASCADE
) COMMENT '模板收藏表';

-- 7. 模板评分表
CREATE TABLE wf_template_rating (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    rating DECIMAL(3,2) NOT NULL COMMENT '评分1-5',
    comment TEXT COMMENT '评价内容',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_template_user_rating (template_id, user_id),
    FOREIGN KEY (template_id) REFERENCES wf_template(id) ON DELETE CASCADE
) COMMENT '模板评分表';

-- 8. 流程执行日志表(用于AI追踪)
CREATE TABLE wf_execution_log (
    id VARCHAR(64) PRIMARY KEY,
    process_instance_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(100) COMMENT '节点ID',
    node_name VARCHAR(255) COMMENT '节点名称',

    -- 执行信息
    event_type VARCHAR(50) COMMENT '事件类型:START/END/ERROR',
    input_data JSON COMMENT '输入数据',
    output_data JSON COMMENT '输出数据',
    error_message TEXT COMMENT '错误信息',

    -- 性能数据
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',

    INDEX idx_process_instance (process_instance_id),
    INDEX idx_node (node_id),
    INDEX idx_start_time (start_time)
) COMMENT '流程执行日志表';
```

### 7.2 MongoDB集合设计(历史数据)

```javascript
// wf_history_process: 流程实例历史
{
    "_id": ObjectId,
    "processInstanceId": "proc_inst_001",
    "processDefinitionKey": "ai_workflow",
    "processDefinitionName": "AI智能工作流",
    "version": 1,

    // 启动信息
    "startTime": ISODate("2026-04-13T10:00:00Z"),
    "startUserId": "user_001",
    "startVariables": {},

    // 结束信息
    "endTime": ISODate("2026-04-13T10:05:00Z"),
    "endStatus": "COMPLETED", // COMPLETED/TERMINATED

    // 节点执行历史
    "nodeHistory": [
        {
            "nodeId": "startEvent",
            "nodeName": "开始",
            "enterTime": ISODate("2026-04-13T10:00:00Z"),
            "leaveTime": ISODate("2026-04-13T10:00:01Z"),
            "duration": 1000
        },
        {
            "nodeId": "llmTask",
            "nodeName": "大模型调用",
            "enterTime": ISODate("2026-04-13T10:00:01Z"),
            "leaveTime": ISODate("2026-04-13T10:02:00Z"),
            "duration": 119000,
            "inputTokens": 500,
            "outputTokens": 200,
            "model": "gpt-4-turbo"
        }
    ],

    // 统计信息
    "totalDuration": 300000,
    "totalNodes": 5,
    "totalInputTokens": 1500,
    "totalOutputTokens": 800,

    // 上下文快照
    "finalContext": {}
}

// wf_history_node: 节点执行历史
{
    "_id": ObjectId,
    "processInstanceId": "proc_inst_001",
    "nodeId": "llmTask",
    "nodeName": "大模型调用",

    // 输入
    "inputVariables": {
        "userQuery": "如何学习Java?",
        "context": {}
    },

    // 输出
    "outputVariables": {
        "answer": "学习Java的步骤...",
        "intent": "direct_answer"
    },

    // 执行时间
    "startTime": ISODate("2026-04-13T10:00:01Z"),
    "endTime": ISODate("2026-04-13T10:02:00Z"),

    // 模型调用详情
    "llmCall": {
        "provider": "openai",
        "model": "gpt-4-turbo",
        "prompt": "系统提示词...",
        "inputTokens": 500,
        "outputTokens": 200,
        "latency": 5000,
        "cost": 0.05
    },

    // 错误信息(如有)
    "error": null
}
```

### 7.3 Redis缓存设计

```
# 缓存键设计规范
# 格式: {prefix}:{entity}:{id}:{field}

# 模板缓存
wf:template:{templateId} -> TemplateDTO (TTL: 1小时)
wf:template:list:category:{categoryId} -> List<TemplateDTO> (TTL: 5分钟)
wf:template:hot -> List<TemplateDTO> (TTL: 10分钟)

# 流程定义缓存
wf:process:def:{processDefId} -> ProcessDefinitionDTO (TTL: 30分钟)

# 用户收藏缓存
wf:favorite:{userId} -> Set<String> (templateIds) (TTL: 15分钟)

# 流量控制
wf:rate:{userId}:{templateId} -> count (TTL: 1分钟, 最大5次/分钟)

# AI会话缓存
wf:ai:session:{sessionId} -> SessionContext (TTL: 30分钟)
```

---

## 八、API接口设计

### 8.1 工作流模板API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workflow/templates | 创建模板 |
| GET | /api/workflow/templates | 查询模板列表 |
| GET | /api/workflow/templates/{id} | 获取模板详情 |
| PUT | /api/workflow/templates/{id} | 更新模板 |
| DELETE | /api/workflow/templates/{id} | 删除模板 |
| POST | /api/workflow/templates/{id}/publish | 发布模板 |
| POST | /api/workflow/templates/{id}/fork | 复制模板 |
| POST | /api/workflow/templates/{id}/rate | 评分模板 |

### 8.2 工作流广场API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/workflow/square/search | 搜索模板 |
| GET | /api/workflow/square/hot | 热门模板 |
| GET | /api/workflow/square/new | 最新模板 |
| GET | /api/workflow/square/recommend | 推荐模板 |
| GET | /api/workflow/square/categories | 获取分类 |
| GET | /api/workflow/square/categories/{id}/templates | 分类模板 |

### 8.3 流程执行API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workflow/instances | 启动流程实例 |
| GET | /api/workflow/instances | 查询实例列表 |
| GET | /api/workflow/instances/{id} | 获取实例详情 |
| POST | /api/workflow/instances/{id}/cancel | 取消实例 |
| GET | /api/workflow/instances/{id}/trace | 获取执行轨迹 |
| POST | /api/workflow/instances/{id}/variables | 设置变量 |

### 8.4 流程定义API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workflow/definitions/deploy | 部署流程 |
| GET | /api/workflow/definitions | 查询定义列表 |
| GET | /api/workflow/definitions/{id} | 获取定义详情 |
| GET | /api/workflow/definitions/{id}/bpmn | 获取BPMN XML |
| GET | /api/workflow/definitions/{id}/svg | 获取SVG图 |
| PUT | /api/workflow/definitions/{id}/state | 修改状态 |
| DELETE | /api/workflow/definitions/{id} | 删除定义 |

---

## 九、实现步骤建议

### 阶段一：基础集成(2周)
1. 引入Flowable依赖，配置数据源
2. 创建Flowable基础配置类
3. 实现流程部署、启动、完成基础功能
4. 集成bpmn-js设计器

### 阶段二：模板功能(2周)
1. 设计并创建模板相关表
2. 实现模板CRUD接口
3. 实现模板版本管理
4. 实现模板搜索与筛选

### 阶段三：广场功能(1周)
1. 实现广场API
2. 实现收藏、评分功能
3. 实现推荐算法

### 阶段四：AI节点集成(2周)
1. 设计AI节点配置表
2. 实现AI节点服务
3. 开发自定义任务处理器
4. 实现与现有Agent模式集成

### 阶段五：监控与优化(1周)
1. 实现流程监控页面
2. 添加执行日志
3. 性能优化(缓存、索引)

---

## 十、参考资料

1. **Flowable官方文档**: https://flowable.com/open-source/docs/
2. **Flowable与Spring Boot 3集成**: https://cxyroad.blog.csdn.net/article/details/153711800
3. **bpmn-js中文文档**: https://github.com/Lindacnatt/bpmn-js-doc
4. **Dify工作流设计**: https://docs.dify.ai/getting-started/intro
5. **Coze工作流设计**: https://www.coze.cn/docs/guides/workflow

---

## 十一、下一步建议

1. **评审确认**：请项目组评审技术选型和架构设计
2. **环境准备**：准备Flowable 7.x、MongoDB、MinIO环境
3. **原型开发**：先实现基础流程设计器Demo
4. **数据迁移**：设计现有数据的迁移方案(如已有对话数据)
5. **安全考虑**：设计工作流权限控制和审计日志

---

*文档版本: v1.0*
*创建时间: 2026-04-13*
*作者: JinGu3 AI Agent*
