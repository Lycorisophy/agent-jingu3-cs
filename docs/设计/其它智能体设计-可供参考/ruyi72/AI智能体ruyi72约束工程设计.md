# 如意72（ruyi72）约束工程设计

## 概述

如意72 的约束工程采用**工作区隔离 + 技能分级 + 路径安全验证**的设计理念。通过工作区边界约束、工具能力约束和技能等级约束，确保智能体在可控范围内操作本地环境。

---

## 一、工作区隔离机制

### 1.1 工作区概念

工作区（Workspace）是智能体操作的根目录限制。所有文件操作必须限定在工作区范围内。

```
工作区结构：
{workspace}/
├── (用户项目文件)
├── workspace/     (ReAct 默认工作子目录)
└── ...

注意：工作区可以是任何本地目录，智能体仅能访问该目录及其子目录。
```

### 1.2 路径安全验证

```python
# src/agent/tools.py

def safe_child(root: Path, relative: str) -> Path:
    """确保子路径不超出工作区根目录"""
    
    rel = (relative or ".").replace("\\", "/").lstrip("/")
    
    # 禁止路径穿越
    if ".." in rel.split("/"):
        raise ToolError("禁止在路径中使用 ..")
    
    # 解析实际路径
    child = (root / rel).resolve()
    root_r = root.resolve()
    
    # 验证路径在工作区内
    try:
        child.relative_to(root_r)
    except ValueError as e:
        raise ToolError("路径超出工作区范围") from e
    
    return child
```

**安全检查步骤**：
1. 禁止 `..` 路径穿越
2. 解析实际绝对路径
3. 验证路径在工作区范围内

### 1.3 工作区验证函数

```python
def _workspace_root(workspace: str) -> Path:
    p = Path(workspace).expanduser()
    if not p.is_absolute():
        p = p.resolve()
    return p

def tool_read_file(workspace: str, path: str) -> str:
    root = _workspace_root(workspace)
    if not root.is_dir():
        return f"错误: 工作区不存在或不是目录: {root}"
    target = safe_child(root, path)
    # ... 后续读取逻辑
```

---

## 二、工具能力约束

### 2.1 可用工具清单

如意72 的 ReAct 模式提供以下工具：

| 工具 | 描述 | 约束 |
|------|------|------|
| `read_file` | 读取工作区文件 | UTF-8 编码，120KB 截断 |
| `list_dir` | 列出目录内容 | 仅工作区内 |
| `write_file` | 创建/覆盖文件 | UTF-8，最大 2MB |
| `run_shell` | 执行 shell 命令 | Windows cmd，120s 超时 |

### 2.2 read_file 约束

```python
def tool_read_file(workspace: str, path: str) -> str:
    root = _workspace_root(workspace)
    if not root.is_dir():
        return f"错误: 工作区不存在或不是目录: {root}"
    target = safe_child(root, path)
    if not target.is_file():
        return f"错误: 不是文件或不存在: {path}"
    try:
        text = target.read_text(encoding="utf-8", errors="replace")
        if len(text) > 120_000:
            return text[:120_000] + "\n…(已截断)"
        return text
    except OSError as e:
        return f"错误: 读取失败 {e!s}"
```

**约束**：
- 文件大小超过 120KB 时截断
- 编码错误时使用 `errors="replace"` 避免崩溃

### 2.3 list_dir 约束

```python
def tool_list_dir(workspace: str, path: str = ".") -> str:
    root = _workspace_root(workspace)
    if not root.is_dir():
        return f"错误: 工作区不存在或不是目录: {root}"
    target = safe_child(root, path)
    if not target.is_dir():
        return f"错误: 不是目录或不存在: {path}"
    # 列出文件/目录，按名称排序
```

**约束**：
- 只能列出工作区内的目录
- 结果按名称排序

### 2.4 write_file 约束

```python
def tool_write_file(workspace: str, path: str, content: str) -> str:
    root = _workspace_root(workspace)
    if not root.is_dir():
        return f"错误: 工作区不存在或不是目录: {root}"
    target = safe_child(root, path)
    # 自动创建父目录
    try:
        target.parent.mkdir(parents=True, exist_ok=True)
    except OSError as e:
        return f"错误: 无法创建父目录 {e!s}"
    # 内容长度限制
    text = content if content is not None else ""
    if len(text) > 2_000_000:
        return "错误: 内容过长（>2MB）"
    # UTF-8 编码，换行符规范化
    target.write_text(text, encoding="utf-8", newline="\n")
    return f"已写入: {path}（{len(text)} 字符）"
```

**约束**：
- 最大 2MB 内容限制
- 自动创建父目录
- UTF-8 编码，换行符规范化

### 2.5 run_shell 约束

```python
def tool_run_shell(workspace: str, command: str) -> str:
    root = _workspace_root(workspace)
    if not root.is_dir():
        return f"错误: 工作区不存在或不是目录: {root}"
    
    cmd = (command or "").strip()
    if not cmd:
        return "错误: 命令为空"
    
    try:
        proc = subprocess.run(
            cmd,
            cwd=str(root),  # 固定工作区为 cwd
            shell=True,
            capture_output=True,
            timeout=120,  # 120秒超时
        )
        # Windows GBK 解码 + UTF-8 回退
        stdout = _decode_shell_bytes(proc.stdout or b"")
        stderr = _decode_shell_bytes(proc.stderr or b"")
        # 输出 80KB 截断
        if len(body) > 80_000:
            body = body[:80_000] + "\n…(已截断)"
    except subprocess.TimeoutExpired:
        return "错误: 命令超时（120s）"
    except OSError as e:
        return f"错误: 执行失败 {e!s}"
```

**约束**：
- 工作目录固定为工作区
- 120 秒超时
- Windows 多编码自动检测
- 输出 80KB 截断
- 命令长度超限提示

---

## 三、技能分级约束

### 3.1 技能等级定义

```python
# src/skills/loader.py

@dataclass(frozen=True)
class SkillMeta:
    id: str        # 例如 "safe/deep-research"
    name: str
    description: str
    level: int      # 0 = safe, 1 = act, 2 = warn_act
    path: Path      # SKILL.md 绝对路径
    extra: dict     # 头部其它字段
```

**等级说明**：

| 等级 | 标识 | 描述 | 使用限制 |
|------|------|------|----------|
| 0 | safe | 只读/查询/分析 | 默认 Chat 模式可用 |
| 1 | act | 可能修改本地文件/状态 | 需谨慎 |
| 2 | warn_act | 高危操作 | 每次需用户确认 |

### 3.2 高危技能确认机制

```markdown
# SAFETY_PROMPT 中的约束：

2. 命令与高危技能
- 对于涉及磁盘、进程、服务、云同步、数据库、剪贴板等高危技能（warn_act 等级）：
  - 先解释要做什么、为什么要做、可能风险；
  - 再显式要求用户确认，例如：「我确认使用 disk-manager 技能 执行 XXX」。
- 未获得明确确认前，不要建议或虚构已执行高危操作。
```

### 3.3 技能加载与过滤

```python
class SkillRegistry:
    @property
    def skills(self) -> list[SkillMeta]:
        return list(self._skills)
    
    def list_by_levels(self, levels: Iterable[int]) -> list[SkillMeta]:
        """按等级过滤技能"""
        level_set = set(levels)
        return [s for s in self._skills if s.level in level_set]
    
    def get_by_name(self, name: str) -> SkillMeta | None:
        return self._by_name.get(name.strip())
```

### 3.4 技能提示词构建

```python
def build_react_skills_block() -> str:
    """为 ReAct system prompt 构建包含全部技能 + 等级的文本块。"""
    
    labels = {
        0: "safe(0) — 只读/查询/分析，默认 Ask 模式可用",
        1: "act(1) — 可能修改本地文件/状态，需要谨慎",
        2: "warn_act(2) — 高危操作（磁盘/进程/服务/云/数据库/剪贴板等），每次需用户确认",
    }
    
    # 按等级分组构建提示词
```

---

## 四、配置级约束

### 4.1 LLM 配置约束

```python
# src/config.py

class LLMConfig(BaseModel):
    provider: LLMProvider = "ollama"
    base_url: str = "http://127.0.0.1:11434"
    model: str = "llama3.2"
    temperature: float = Field(default=0.6, ge=0.0, le=2.0)  # 范围约束
    max_tokens: int = Field(default=2048, ge=1, le=262144)     # 范围约束
    api_mode: Literal["native", "openai"] = "native"
    trust_env: bool | None = None
```

### 4.2 ReAct 步数约束

```python
class AgentConfig(BaseModel):
    react_max_steps_default: int = Field(default=8, ge=1, le=200)  # 默认 8 步，上限 200
```

### 4.3 应用窗口约束

```python
class AppConfig(BaseModel):
    title: str = _DEFAULT_APP_TITLE
    width: int = Field(default=960, ge=400, le=4096)   # 宽度 400-4096
    height: int = Field(default=640, ge=300, le=4096)  # 高度 300-4096
    debug: bool = False
```

---

## 五、上下文压缩约束

### 5.1 上文压缩配置

```python
# src/config.py

class ContextCompressionConfig(BaseModel):
    """上文压缩：检查点 + 摘要；不修改 messages.json 全文，仅影响喂给模型的视图。"""
    
    enabled: bool = False
    context_token_budget: int = Field(default=32000, ge=4096, le=500_000)
    pre_send_threshold: float = Field(default=0.85, ge=0.3, le=0.99)
    max_summary_chars: int = Field(default=8000, ge=500, le=200_000)
    summary_max_tokens: int = Field(default=1024, ge=128, le=8192)
    max_message_chars_phase_a: int = Field(default=24000, ge=2000, le=500_000)
    idle_compress_interval_sec: int = Field(default=0, ge=0, le=86400)
    post_reply_compress: bool = False
```

### 5.2 压缩约束参数

| 参数 | 默认值 | 范围 | 说明 |
|------|--------|------|------|
| context_token_budget | 32000 | 4096-500000 | Token 预算上限 |
| pre_send_threshold | 0.85 | 0.3-0.99 | 触发压缩阈值 |
| max_summary_chars | 8000 | 500-200000 | 单次摘要最大字符 |
| summary_max_tokens | 1024 | 128-8192 | 摘要生成 token 上限 |
| max_message_chars_phase_a | 24000 | 2000-500000 | 单条消息截断上限 |

---

## 六、记忆提取约束

### 6.1 记忆提取配置

```python
class MemoryConfig(BaseModel):
    extract_llm_timeout_sec: int = Field(default=300, ge=60, le=3600)       # LLM 超时
    extract_max_input_chars: int = Field(default=16000, ge=2000, le=500000)  # 输入上限
    extract_max_tokens: int = Field(default=4096, ge=256, le=131072)         # 输出上限
```

### 6.2 提取约束参数

| 参数 | 默认值 | 范围 | 说明 |
|------|--------|------|------|
| extract_llm_timeout_sec | 300 | 60-3600 | 读取超时（秒） |
| extract_max_input_chars | 16000 | 2000-500000 | 单次输入上限 |
| extract_max_tokens | 4096 | 256-131072 | 生成 token 上限 |

---

## 七、约束工程最佳实践

### 7.1 防御性编程

- 所有路径操作前验证工作区边界
- 所有工具调用捕获异常并返回可读错误
- 超大文件/输出自动截断

### 7.2 失败透明原则

```markdown
# SAFETY_PROMPT:
- 不要隐藏失败，命令或推理失败时，直接说明原因并给出可行的下一步。
```

### 7.3 高危操作确认

- warn_act 技能必须显式要求用户确认
- 禁止虚构已执行的高危操作
- 删除/覆盖操作前提示备份建议

---

## 总结

如意72 的约束工程具有以下特点：

1. **工作区隔离**：通过 `safe_child()` 函数防止路径遍历攻击
2. **工具能力约束**：每个工具都有明确的大小限制和超时控制
3. **技能分级管控**：warn_act 技能需要用户显式确认
4. **配置级约束**：Pydantic Field 验证确保配置在合理范围
5. **上下文压缩**：避免超长上下文导致模型性能下降
6. **记忆提取约束**：防止超大输入导致 LLM 超时