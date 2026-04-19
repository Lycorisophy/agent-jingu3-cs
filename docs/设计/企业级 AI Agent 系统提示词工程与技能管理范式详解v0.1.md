# 企业级 AI Agent 系统提示词工程与技能管理范式详解v0.1

本篇章聚焦 **系统提示词（System Prompt）** 和 **技能工具（Skills/Tools）** 的全生命周期管理。我们将在双表双模型上下文工程的框架下，进一步阐述如何实现提示词与工具的**版本化、场景化披露、动态装配**，从而让 Agent 在生产环境中持续演进而不破坏既有会话。

---

## 一、核心挑战

在复杂的 AI Agent 系统中，系统提示词与工具管理面临以下矛盾：

| 挑战 | 典型场景 | 潜在后果 |
|:---|:---|:---|
| **版本迭代与历史会话的冲突** | 提示词 v2 增加了“禁止回答政治问题”，但用户恢复 3 天前的会话（基于 v1） | 行为不一致或约束缺失 |
| **工具升级与旧调用链的断裂** | `get_weather` 工具 v2 返回格式从 JSON 改为自然语言 | 依赖旧格式的 Agent 逻辑报错 |
| **上下文窗口有限但技能数量膨胀** | 企业累积 200+ 工具，若全部描述塞入提示词 | Token 爆炸、LLM 选择困难 |
| **不同场景需要不同的工具集** | Ask 模式禁止写文件，Win11 环境不应暴露 macOS 专属命令 | 误调用导致安全隐患或用户体验差 |
| **用户需要了解 Agent 能力但信息过载** | 前端展示 50 个技能卡片，用户迷失 | 学习成本高，使用率低 |

解决方案：建立一套**集中式、版本化、场景感知**的提示词与技能管理系统。

---

## 二、总体架构

```text
┌─────────────────────────────────────────────────────────────────┐
│                     提示词/技能管理中心 (Prompt & Skill Registry) │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐ │
│  │ 提示词版本库          │  │ 技能版本库 (ToolSpecification)     │ │
│  │ - 模板 + 变量插槽     │  │ - name, description, 参数 schema  │ │
│  │ - 语义版本号          │  │ - 适用场景约束 (mode, os, auth)   │ │
│  └──────────────────────┘  └──────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│            Agent 运行时装配层 (Runtime Assembly)                  │
│  - 根据 Session State 中的版本指针拉取具体版本内容                 │
│  - 根据当前上下文 (mode, 用户平台) 过滤技能列表                    │
│  - 实施渐进式披露：先给元数据，需要时给完整描述                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Agent 执行上下文                          │
│  - SystemMessage (来自注册中心的指定版本)                         │
│  - Tools (动态 ToolProvider 按场景过滤后返回)                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 三、系统提示词版本化管理

### 3.1 设计原则：**存指针，不存内容**

在会话状态（`langgraph_checkpoints.state_data`）中，**仅存储提示词版本号**，而非完整文本。

```json
{
  "system_prompt_version": "v2.1.0",
  // 其他状态...
}
```

### 3.2 版本号规范

采用**语义化版本**（Semantic Versioning）：`v{major}.{minor}.{patch}`

| 版本段 | 递增条件 | 示例 |
|:---|:---|:---|
| **major** | 不兼容的 API 变更（如移除某个变量插槽） | `v1.x.x` → `v2.0.0` |
| **minor** | 向后兼容的功能新增（如增加可选指令） | `v2.1.0` → `v2.2.0` |
| **patch** | 向后兼容的文本微调（如修正错别字） | `v2.1.1` → `v2.1.2` |

### 3.3 提示词注册中心表设计

```sql
CREATE TABLE system_prompt_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(20) NOT NULL UNIQUE,        -- 如 'v2.1.0'
    template TEXT NOT NULL,                     -- 包含占位符的模板
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    INDEX idx_version (version)
);
```

**模板示例**：
```text
你是一个专业的 AI 助手，名字叫 {{assistant_name}}。
当前用户是 {{user_name}}，角色为 {{user_role}}。
你必须遵守以下规则：
1. 回答简洁专业。
2. 若不知道答案，请明确说明。
3. 不要回答任何关于 {{forbidden_topics}} 的问题。
```

### 3.4 运行时注入流程

```java
public class PromptAssembler {
    @Autowired
    private PromptRegistry registry;
    
    public String assembleSystemPrompt(AgentState state, Map<String, Object> variables) {
        String version = state.getSystemPromptVersion();
        String template = registry.getTemplate(version);
        return renderTemplate(template, variables);
    }
}
```

### 3.5 版本升级与兼容性策略

| 场景 | 处理策略 |
|:---|:---|
| **新会话** | 使用当前最新的 active 版本 |
| **旧会话恢复** | 继续使用会话记录中的版本号拉取对应模板（即使该版本已非最新） |
| **major 升级不兼容** | 提示用户“系统已升级，建议开启新对话”，或自动迁移旧状态 |
| **patch 升级** | 可在下次恢复时无缝替换为新模板（因为完全兼容） |

---

## 四、技能工具版本化管理

### 4.1 技能元数据模型

每个工具定义包含以下核心字段：

```java
public class SkillDefinition {
    private String name;                // 唯一标识
    private String version;             // 语义版本号
    private String displayName;         // 前端展示名称
    private String shortDescription;    // 一句话描述（渐进式披露第一层）
    private String fullDescription;     // 完整描述（给 LLM 看）
    private JsonSchema inputSchema;     // 参数定义
    private Set<SkillConstraint> constraints; // 场景约束
    private String implementationRef;   // 指向实际执行代码的引用
}
```

### 4.2 约束定义（场景披露）

```java
public class SkillConstraint {
    public enum ConstraintType { MODE, OS, USER_ROLE, CUSTOM }
    
    private ConstraintType type;
    private List<String> allowedValues; // e.g., ["ask", "plan"], ["windows", "linux"]
    private String expression;          // 复杂规则可用 SpEL
}
```

**示例**：
- 文件写入工具：`constraints: [{type: MODE, allowedValues: ["plan"]}]`
- macOS 专属命令：`constraints: [{type: OS, allowedValues: ["macos"]}]`
- 管理员工具：`constraints: [{type: USER_ROLE, allowedValues: ["admin"]}]`

### 4.3 工具注册表表设计

```sql
CREATE TABLE skill_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    version VARCHAR(20) NOT NULL,
    display_name VARCHAR(100),
    short_description VARCHAR(200),
    full_description TEXT,
    input_schema JSON,
    constraints JSON,                   -- 约束列表 JSON
    implementation_ref VARCHAR(200),    -- 如 beanName:method
    is_deprecated BOOLEAN DEFAULT FALSE,
    successor_version VARCHAR(20),      -- 若废弃，指向新版本
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name_version (name, version),
    INDEX idx_name (name)
);
```

### 4.4 工具版本指针存储

在 B 表 (`langgraph_checkpoints.state_data`) 中记录：

```json
{
  "tool_versions": {
    "read_file": "v1.2.0",
    "search_web": "v2.0.1"
  }
}
```

### 4.5 运行时动态装配（结合场景约束）

```java
@Component
public class ContextAwareToolProvider implements ToolProvider {
    @Autowired
    private SkillRegistry registry;
    
    @Override
    public List<ToolSpecification> provideTools(ChatMemory memory, UserMessage userMessage) {
        ExecutionContext ctx = ContextHolder.get();
        String mode = ctx.getMode();
        String os = ctx.getUserPlatform();
        String role = ctx.getUserRole();
        
        // 1. 获取会话记录的工具版本指针
        Map<String, String> toolVersions = ctx.getState().getToolVersions();
        
        // 2. 拉取对应版本的工具定义，并过滤约束
        return toolVersions.entrySet().stream()
            .map(e -> registry.get(e.getKey(), e.getValue()))
            .filter(skill -> skill.satisfiesConstraints(mode, os, role))
            .map(skill -> skill.toToolSpecification(ctx.isProgressiveDisclosure()))
            .collect(Collectors.toList());
    }
}
```

---

## 五、渐进式披露与场景披露设计

### 5.1 三层信息模型

借鉴 Anthropic Skills 理念，将每个技能的信息分为三层：

| 层级 | 内容 | 何时发送给 LLM | Token 消耗 |
|:---|:---|:---|:---|
| **L1: 元数据层** | `name` + `short_description` | 每次会话始终加载 | 极小 |
| **L2: 核心指令层** | `full_description` + `input_schema` | 当用户意图涉及该技能时 | 中等 |
| **L3: 参考资源层** | 代码示例、详细文档、使用案例 | 仅当 Agent 明确请求时 | 较大 |

### 5.2 实现方式：动态 ToolProvider 与 LLM 主动请求

**阶段一：仅暴露 L1**
```java
// ToolProvider 返回的 ToolSpecification 仅包含 name 和简短 description
ToolSpecification.builder()
    .name("read_file")
    .description("读取本地文件内容。")  // 仅 L1
    .build();
```

**阶段二：LLM 需要时，通过特殊工具请求详情**
```java
@Tool("获取指定技能的详细使用说明")
public String getSkillDetails(@P("技能名称") String skillName) {
    Skill skill = registry.getLatest(skillName);
    return skill.getFullDescription() + "\n\n参数说明:\n" + skill.getInputSchema();
}
```

### 5.3 前端渐进式披露

对应 API 设计两个接口：

- `GET /api/skills` → 返回 L1 信息（用于卡片列表）
- `GET /api/skills/{name}` → 返回 L1+L2 信息（用于详情页/参数表单）

---

## 六、完整的表结构汇总

### 6.1 提示词管理

```sql
-- 提示词版本表
CREATE TABLE prompt_registry (
    version VARCHAR(20) PRIMARY KEY,
    template TEXT NOT NULL,
    variables JSON,               -- 可用的插槽变量列表
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP
);
```

### 6.2 技能注册表

```sql
CREATE TABLE skill_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_name VARCHAR(100) NOT NULL,
    version VARCHAR(20) NOT NULL,
    display_name VARCHAR(100),
    short_desc VARCHAR(200),
    full_desc TEXT,
    input_schema JSON,
    constraints JSON,
    impl_ref VARCHAR(200),
    is_deprecated BOOLEAN,
    successor_version VARCHAR(20),
    created_at TIMESTAMP,
    UNIQUE KEY uk_name_ver (skill_name, version)
);
```

### 6.3 会话状态表（B 表）版本指针字段

```sql
-- langgraph_checkpoints 表中 state_data 字段 JSON 结构示例
{
  "messages": [...],
  "system_prompt_version": "v2.1.0",
  "tool_versions": {
    "read_file": "v1.2.0",
    "search_web": "v2.0.1"
  }
}
```

### 6.4 用户可见技能清单表（可选，用于前端展示）

```sql
CREATE TABLE user_visible_skills (
    skill_name VARCHAR(100),
    version VARCHAR(20),
    category VARCHAR(50),
    icon_url VARCHAR(200),
    order_index INT,
    PRIMARY KEY (skill_name, version)
);
```

---

## 七、工程实践流程

### 7.1 新增/升级提示词流程

1. 在 `prompt_registry` 表中插入新版本记录。
2. 标记旧版本 `is_active = false`（如需废弃）。
3. 新会话自动使用最新 active 版本。
4. 旧会话恢复时仍读取其记录的版本号（兼容）。

### 7.2 新增/升级技能流程

1. 开发实现类，并定义 `@Tool` 注解。
2. 在 `skill_definitions` 表中注册元数据（含版本号、约束）。
3. 如需废弃旧版，设置 `is_deprecated = true` 并指定 `successor_version`。
4. 运行时，若会话记录的版本已废弃，尝试自动映射到后继版本（若兼容）或提示用户。

### 7.3 场景约束测试矩阵

每个技能上线前，应验证其在不同模式、平台、角色下的可见性与正确性。可建立自动化测试用例覆盖约束组合。

---

## 八、范式收益总结

| 能力 | 传统硬编码方式 | 本范式 |
|:---|:---|:---|
| **提示词热更新** | 需重启服务 | 配置中心推送 + 版本指针无缝切换 |
| **工具平滑升级** | 旧会话报错 | 按版本号拉取对应实现，可降级提示 |
| **场景化工具过滤** | 需手写 if-else | 声明式约束，运行时自动过滤 |
| **Token 成本控制** | 所有工具描述全量塞入 | 渐进式披露，节省 70%+ 上下文 |
| **用户技能发现** | 硬编码 UI | API 动态生成技能卡片，支持搜索分类 |

---

## 九、与其他模块的关系

- **双表双模型上下文工程**：版本指针存储在 B 表 `state_data` 中。
- **意图识别与模式路由**：为场景约束提供当前模式 (`mode`)。
- **工具执行引擎**：根据 `impl_ref` 动态调用实际业务代码。

此范式与整体架构无缝集成，构成了企业级 Agent 系统的“配置管理”中枢，确保系统在快速迭代中保持稳定、可控、可观测。