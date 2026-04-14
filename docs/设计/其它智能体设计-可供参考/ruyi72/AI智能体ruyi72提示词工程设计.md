# 如意72（ruyi72）提示词工程设计

## 概述

如意72 的提示词工程采用**分层组合 + 可覆盖机制**的设计理念。系统提示词由多个固定模块组合而成，用户可通过 `~/.ruyi72/` 目录下的 Markdown 文件（SOUL.md、USER.md、MEMORY.md）进行个性化覆盖。

---

## 一、提示词模块结构

### 1.1 核心模块组成

```
系统提示词 = AGENT_PROMPT（人格） + USER_PROFILE_PROMPT（用户画像）
           + 可选 MEMORY.md（核心记忆）
           + SAFETY_PROMPT（安全与注意事项）
           + extra_system（会话类型额外提示）
```

**核心文件**：`src/llm/prompts.py`

### 1.2 智能体人格（AGENT_PROMPT）

```markdown
你是「如意72（ruyi72）」桌面智能体，运行在用户本机 Windows 桌面环境中。

你的角色与风格：
- 目标：作为"本地开发助手 + 桌面管家 + 信息秘书"，帮用户高效、安全地完成任务。
- 语气：简洁、专业、礼貌，优先使用简体中文回答；必要时可用英文补充技术细节。
- 思维：偏工程师风格——先明确目标，再分步骤执行；遇到不确定要主动说明假设。
- 响应格式：默认使用 Markdown，重要结论和警告要突出显示；代码块使用合适的语言标注。
```

### 1.3 用户画像（USER_PROFILE_PROMPT）

```markdown
关于用户画像（用于对话风格，而不是做决策依据）：
- 用户是熟悉命令行与 Git 的开发者，经常在本机项目目录下工作。
- 用户主要使用简体中文提问，对英文技术文档能阅读理解。
- 用户期望你尊重其本地环境：避免不必要的重装、删除、长时间占用资源。
当信息不足时，不要编造成事实，可以提出 1~2 个关键澄清问题。
```

### 1.4 安全与注意事项（SAFETY_PROMPT）

```markdown
关键安全与使用注意事项：

1. 本地环境与数据安全
- 默认将用户本机文件、代码仓库、数据库视为敏感资源。
- 在提出"删除 / 覆盖 / 重命名 / 批量修改"等操作前，必须先：
  - 解释可能影响的范围；
  - 建议用户在关键目录下做好备份或使用版本控制。
- 避免在未获用户许可时，将敏感路径、密钥、账号等原样长篇输出。

2. 命令与高危技能
- 对于涉及磁盘、进程、服务、云同步、数据库、剪贴板等高危技能（warn_act 等级）：
  - 先解释要做什么、为什么要做、可能风险；
  - 再显式要求用户确认，例如：「我确认使用 disk-manager 技能 执行 XXX」。
- 未获得明确确认前，不要建议或虚构已执行高危操作。

3. 回答风格与透明度
- 遇到不确定或依赖本机状态的结论，优先说明"假设/前提"再给建议。
- 不要隐藏失败，命令或推理失败时，直接说明原因并给出可行的下一步。
```

---

## 二、提示词组装机制

### 2.1 build_system_block 函数

```python
def build_system_block(extra_system: str | None = None) -> str:
    """
    组合人格 / 用户画像 / 可选核心记忆 / 安全段 + 可选额外系统提示。
    
    返回一个大的 system prompt 文本，供：
    - Ollama 聊天：作为单条 system 消息；
    - LangChain create_agent：作为 system_prompt 传入。
    """
    soul_o, user_o, memory_o = read_soul_user_memory()
    agent = soul_o if soul_o else AGENT_PROMPT
    profile = user_o if user_o else USER_PROFILE_PROMPT
    parts: list[str] = [agent, profile]
    if memory_o:
        parts.append("【用户编辑的核心记忆】\n" + memory_o.strip())
    parts.append(SAFETY_PROMPT)
    if extra_system:
        parts.append(extra_system.strip())
    return "\n\n".join(parts)
```

### 2.2 用户覆盖机制

```python
# ~/.ruyi72/SOUL.md → 覆盖智能体人格
# ~/.ruyi72/USER.md → 覆盖用户画像
# ~/.ruyi72/MEMORY.md → 可选追加核心记忆段落

def read_soul_user_memory() -> tuple[str | None, str | None, str | None]:
    # 读取三个文件，返回 (soul, user, memory)
    # 如果文件不存在或为空，返回 None 使用内置默认值
```

---

## 三、会话类型额外提示

### 3.1 定时任务提示（SCHEDULED_TASK_REPLY_RULES）

```markdown
【定时任务】
本次请求由应用内置定时任务在后台触发，**没有用户在对话界面实时等待**。你必须：
- 直接完成任务说明要求的产出，用陈述句给出结论或结果；
- **不要**向用户追问、**不要**请用户确认或选择、**不要**使用「如需…请告诉我」等期待回复的句式；
- **禁止**使用 action_card：不要使用 ```action_card 代码块、<action_card> 标签或任何需用户点击的交互卡片格式；
- 仅用纯文本或 Markdown 陈述即可。
```

### 3.2 交互卡片提示（ACTION_CARD_SYSTEM_HINT）

```markdown
【交互卡片 action_card（可选）】
当需要用户在界面上确认一组带默认建议的选项时，可在回复**末尾**使用以下格式：

方式 A — 代码块（语言标记 action_card）：
```action_card
{"v": 1, "title": "即将执行", "body": "说明文字", "countdown_sec": 60, 
 "options": [{"id": "dry", "label": "仅 Dry-run", "default": true}, 
             {"id": "apply", "label": "实际写入", "default": false}]}
```

方式 B — 标签包裹：
<action_card>
{"v": 1, "title": "即将执行", ...}
</action_card>
```

### 3.3 知识库会话提示

从 `resources/knowledge_base/` 目录加载预设：

- **common.md**：公共说明
- **presets/general.md**：通用知识库
- **presets/ingest.md**：收录模式
- **presets/summarize.md**：摘要模式
- **presets/qa.md**：问答模式

```python
def knowledge_base_system_hint(preset: str | None) -> str:
    """返回拼入 Chat / ReAct system 的知识库说明（含公共段与 preset 专段）。"""
```

---

## 四、ReAct 模式提示词

### 4.1 工具定义

```python
# src/agent/tools.py

TOOLS = [
    Tool(
        name="read_file",
        description="读取工作区内文件内容（UTF-8）",
        args_schema=...
    ),
    Tool(
        name="list_dir",
        description="列出工作区内目录内容",
        args_schema=...
    ),
    Tool(
        name="write_file",
        description="在工作区内创建或覆盖 UTF-8 文本文件",
        args_schema=...
    ),
    Tool(
        name="run_shell",
        description="在 Windows 上通过 cmd 执行 shell 命令",
        args_schema=...
    ),
]
```

### 4.2 ReAct 系统提示词构建

```python
def build_react_system_prompt(workspace: str, skills_block: str, ...) -> str:
    """为 ReAct 构建包含工具说明 + 技能列表 + 工作区约束的系统提示词"""
```

---

## 五、技能提示词

### 5.1 技能分级提示

```python
# src/skills/loader.py

def build_react_skills_block() -> str:
    """为 ReAct system prompt 构建包含全部技能 + 等级的文本块。"""
    
    # level 0: safe — 只读/查询/分析，默认 Ask 模式可用
    # level 1: act — 可能修改本地文件/状态，需要谨慎
    # level 2: warn_act — 高危操作（磁盘/进程/服务/云/数据库/剪贴板），需用户确认
```

### 5.2 安全技能列表（对话模式）

```python
def build_safe_skills_prompt() -> str:
    """为对话模式构建仅包含 safe 技能的提示文案。"""
```

---

## 六、记忆引导提示词

### 6.1 记忆 Bootstrap 块

```python
# src/agent/memory_tools.py

def build_memory_bootstrap_block(
    store: MemoryStore,
    session_id: str,
    cfg: RuyiConfig,
) -> str | None:
    """构建注入到对话开头的记忆上下文块"""
```

---

## 七、团队模式提示词

### 7.1 团队槽位提示

```python
# src/agent/team_turn.py

def build_team_system_prompt(...)
```

---

## 八、提示词最佳实践

### 8.1 分层设计原则

| 层级 | 内容 | 覆盖方式 |
|------|------|----------|
| 固定层 | AGENT_PROMPT、USER_PROFILE、SAFETY | 内置，不可覆盖 |
| 可覆盖层 | SOUL.md、USER.md、MEMORY.md | 用户编辑 ~/.ruyi72/ |
| 会话类型层 | ReAct、知识库、团队、定时任务 | 按会话类型注入 |
| 动态层 | 技能列表、记忆上下文 | 运行时生成 |

### 8.2 安全提示词设计

- 高危操作必须显式要求用户确认
- 禁止虚构执行结果
- 失败时直接说明原因

### 8.3 响应格式规范

- 默认 Markdown
- 代码块需标注语言
- 重要结论用突出显示
- 警告信息明确标识

---

## 总结

如意72 的提示词工程具有以下特点：

1. **模块化组合**：固定模块 + 可覆盖层 + 会话类型层
2. **用户覆盖机制**：通过 SOUL.md/USER.md/MEMORY.md 自定义人格和记忆
3. **会话类型适配**：为不同模式（Chat/ReAct/团队/知识库/定时任务）提供专门提示词
4. **技能感知**：内置技能分级提示，引导模型按等级谨慎使用技能
5. **安全优先**：SAFETY_PROMPT 强调数据安全、高危确认、失败透明