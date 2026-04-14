# 如意72（ruyi72）上下文工程设计

## 概述

如意72 的上下文工程采用**记忆分层存储 + 事件驱动抽取 + Bootstrap 注入**的设计理念。通过 facts（事实）、events（事件）、relations（关系）三类记忆结构，实现跨会话的长期记忆管理；同时支持用户编辑的核心记忆（MEMORY.md）和手写记忆覆盖。

---

## 一、记忆系统架构

### 1.1 记忆数据模型

```python
# src/storage/memory_store.py

@dataclass
class Fact:
    id: str
    created_at: str
    source: str
    key: str          # 例: "user.home_province"
    value: str        # 例: "安徽"
    summary: str      # 例: "用户说自己是安徽人"
    confidence: float # 0.0-1.0
    tags: list[str]
    tier: str         # "trivial" | "important" | "permanent"
    identity_target: str  # "user" | "soul" | "memory"
    merge_hint: str

@dataclass
class Event:
    id: str
    created_at: str
    time: str
    location: str
    actors: list[str]
    action: str
    result: str
    metadata: dict
    source_session_id: str
    
    # v3.0 扩展字段
    subject_actors: list[str]  # 主体
    object_actors: list[str]  # 客体
    triggers: list[str]       # 触发词
    assertion: str            # "actual" | "negative" | "possible" | "not_occurred"
    world_kind: str          # "real" | "fictional" | "hypothetical" | "unknown"
    temporal_kind: str       # "past" | "present" | "future_planned" | "future_uncertain" | "atemporal"
    planned_window: dict     # 计划时间窗口

@dataclass
class EventRelation:
    id: str
    created_at: str
    event_a_id: str
    event_b_id: str
    relation_type: int       # 1-11 (见下表)
    explanation: str
    relation_legacy: str

@dataclass
class PendingIdentityMerge:
    """永驻事实待合并队列（不直接写 USER/SOUL/MEMORY.md）"""
    id: str
    created_at: str
    identity_target: str     # "user" | "soul" | "memory"
    key: str
    value: str
    summary: str
    merge_hint: str
    confidence: float
    tags: list[str]
    source_session_id: str
```

### 1.2 事件关系类型

| 类型码 | 标签 | 说明 |
|--------|------|------|
| 0 | 无关系 | 不落库 |
| 1 | 因果 | 因→果 |
| 2 | 果因 | 果←因 |
| 3 | 前后时序 | 早于 |
| 4 | 后前时序 | 晚于 |
| 5 | 条件 | 前提→结果 |
| 6 | 逆条件 | 结果←前提 |
| 7 | 目的 | 手段→目标 |
| 8 | 逆目的 | 目标←手段 |
| 9 | 子事件 | 子→父 |
| 10 | 父事件 | 父→子 |
| 11 | 其它关系 | 需说明 |

### 1.3 世界层与时间层

```python
# world_kind: 事件所属的世界类型
"real"       # 真实世界（默认）
"fictional"  # 虚构/角色扮演
"hypothetical"  # 假设/思想实验
"unknown"     # 无法区分

# temporal_kind: 事件的时间类型
"past"              # 过去事件（默认）
"present"           # 当前状态
"future_planned"   # 计划中
"future_uncertain"  # 未来不确定
"atemporal"         # 无时间性
```

---

## 二、记忆存储机制

### 2.1 JSONL 文件存储

```python
class MemoryStore:
    """简单的 JSONL 文件存储，不做检索，仅负责持久化。"""
    
    def __init__(self, root: Path | None = None) -> None:
        if root is None:
            root = Path.home() / ".ruyi72" / "memory"
        self._root = root
        self._root.mkdir(parents=True, exist_ok=True)
    
    def _path_for(self, kind: MemoryKind) -> Path:
        return self._root / f"{kind}.jsonl"
    
    def append_facts(self, facts: Iterable[Fact]) -> int:
        return self._append("facts", facts)
    
    def append_events(self, events: Iterable[Event]) -> int:
        return self._append("events", events)
    
    def append_relations(self, relations: Iterable[EventRelation]) -> int:
        return self._append("relations", relations)
    
    def append_pending_identity(self, items: Iterable[PendingIdentityMerge]) -> int:
        path = self._root / "pending_identity.jsonl"
        # ... 追加写入
```

**存储结构**：
```
~/.ruyi72/memory/
├── facts.jsonl
├── events.jsonl
├── relations.jsonl
└── pending_identity.jsonl
```

### 2.2 SQLite 可选存储

```python
# 配置支持 jsonl | sqlite | dual

class MemoryConfig(BaseModel):
    backend: Literal["jsonl", "sqlite", "dual"] = "jsonl"
    sqlite_path: str = ""  # 空则使用 ~/.ruyi72/memory/memory.db
    vector_enabled: bool = False  # 向量索引
    messages_index_enabled: bool = False  # FTS 索引
```

---

## 三、记忆抽取机制

### 3.1 抽取系统提示词

```python
# src/agent/memory_extractor.py

EXTRACT_SYSTEM_PROMPT = """
你是一个记忆抽取助手。现在给你一段中文或英文文本，请你从中提取三类结构化记忆：

1. facts：事实（主要关于用户本身的稳定特征 / 偏好 / 约定）
   每条事实可增加字段：
   - tier：trivial | important | permanent（默认 important）
   - identity_target：user | soul | memory（当 tier 为 permanent 时）
   - merge_hint：合并到 Markdown 时的提示

2. events：事件
   字段：time, location, actors, action, result, metadata
   v3.0 扩展：subject_actors, object_actors, triggers, assertion
             world_kind, temporal_kind, planned_window

3. relations：事件之间的关系
   relation_type: 0-11（见类型表）
   explanation: 说明
"""
```

### 3.2 抽取流程

```python
def extract_and_store_from_text(cfg, text, *, source_session_id=None):
    # 1. 输入验证
    n_chars = len(text)
    max_in = int(cfg.memory.extract_max_input_chars)
    if n_chars > max_in:
        return {"error": f"粘贴内容过长（{n_chars} 字符），超过上限 {max_in}"}
    
    # 2. 调用 LLM 抽取
    client = OllamaClient(cfg.llm)
    messages = [
        {"role": "system", "content": EXTRACT_SYSTEM_PROMPT},
        {"role": "user", "content": text},
    ]
    reply = client.chat(messages, read_timeout_sec=timeout_sec, ...)
    
    # 3. 解析 JSON
    data = json.loads(reply)
    
    # 4. 分类处理
    for item in data.get("facts", []):
        tier = _normalize_tier(item.get("tier"))
        if tier == "trivial": continue
        if tier == "permanent":
            # 加入 pending_identity 队列
            store.append_pending_identity([...])
        else:
            store.append_facts([...])
    
    # 5. 向量索引（如启用）
    if cfg.memory.vector_enabled:
        _index_important_facts_vector(cfg, facts)
        _index_events_vector(cfg, events)
```

### 3.3 抽取约束

| 参数 | 默认值 | 说明 |
|------|--------|------|
| extract_llm_timeout_sec | 300s | LLM 读超时 |
| extract_max_input_chars | 16000 | 输入上限 |
| extract_max_tokens | 4096 | 输出上限 |
| fact_confidence_threshold | 0.7 | 事实置信度阈值 |

---

## 四、Bootstrap 机制（冷启动）

### 4.1 Bootstrap 块构建

```python
# src/agent/memory_tools.py

def build_memory_bootstrap_block(
    store: MemoryStore,
    session_id: str,
    cfg: RuyiConfig,
) -> str | None:
    """构建注入到对话开头的记忆上下文块"""
    
    # 1. 读取近期 facts（按置信度排序）
    recent_facts = store.read_recent_facts(limit=20, ...)
    
    # 2. 读取近期 events（排除 fictional 如配置）
    recent_events = store.read_recent_events(...)
    
    # 3. 读取计划事件（如启用）
    if cfg.memory.bootstrap_planned_summary_enabled:
        planned = store.read_planned_events(...)
    
    # 4. 构建格式化的 bootstrap 块
    return formatted_block
```

### 4.2 Bootstrap 配置

```python
class MemoryConfig(BaseModel):
    # 冷启动排除 fictional 事件（v3.0）
    bootstrap_exclude_fictional_events: bool = True
    # 单独展示「近期计划」摘要
    bootstrap_planned_summary_enabled: bool = True
    bootstrap_planned_events_max: int = Field(default=3, ge=0, le=20)
    # fictional 事件向量索引
    vector_index_fictional_events: bool = False
```

---

## 五、Pending Identity 机制

### 5.1 永驻事实流程

```
用户说：「我来自安徽合肥」
    ↓
记忆抽取 → tier="permanent", identity_target="user"
    ↓
写入 pending_identity.jsonl
    ↓
前端 / API 查看待合并队列
    ↓
用户确认 → 合并到 USER.md
```

### 5.2 API 接口

```python
# app.py Api 类

def list_pending_identity_merges(self, limit=None) -> dict:
    """列出永驻记忆待合并队列"""
    return {"ok": True, "items": store.read_recent_pending_identity(lim)}

def preview_pending_identity_merge(self, pending_id: str) -> dict:
    """预览合并效果"""

def apply_pending_identity_merge(self, pending_id: str) -> dict:
    """应用合并到 USER.md / SOUL.md / MEMORY.md"""
```

---

## 六、会话上下文管理

### 6.1 多会话服务

```python
# src/service/conversation.py

class ConversationService:
    def __init__(self, cfg, store, *, react_default_steps):
        self._active_id: str | None = None
        self._messages: list[dict] = []
        self._meta: SessionMeta | None = None
        self._memory_bootstrap_pending = False
```

### 6.2 对话相位状态

```python
# src/service/dialogue_phase.py

DialoguePhase = Literal[
    "idle",           # 空闲
    "streaming",      # 流式输出中
    "react_running",  # ReAct 运行中
    "team_running",   # 团队运行中
    "followup_pending",  # 后续处理中
]

def set_dialogue_phase(self, phase, *, last_turn_id=None, emit_event=True):
    """更新当前会话的对话相位"""
```

### 6.3 上下文压缩

```python
# src/agent/context_compression.py

class ContextCompressionConfig(BaseModel):
    enabled: bool = False
    context_token_budget: int = 32000    # Token 预算上限
    pre_send_threshold: float = 0.85     # 触发阈值
    max_summary_chars: int = 8000         # 摘要上限
    summary_max_tokens: int = 1024        # 摘要 token 上限
```

---

## 七、闲时自动抽取

### 7.1 自动抽取配置

```python
class MemoryAutoExtractConfig(BaseModel):
    """闲时从会话历史自动抽取记忆（游标去重）"""
    
    enabled: bool = False                    # 默认关闭
    interval_sec: int = Field(default=180)   # 检查间隔
    max_chars_per_batch: int = 16000        # 每批最大字符
    min_chars_to_extract: int = 40           # 最小触发长度
    max_sessions_scanned: int = 30           # 每次扫描会话数
```

### 7.2 游标机制

```python
# 游标持久化在 ~/.ruyi72/memory_auto_extract_state.json
# 避免重复抽取同一会话
```

---

## 八、上下文工程最佳实践

### 8.1 记忆分层存储

| 类型 | 用途 | 更新频率 |
|------|------|----------|
| Fact (important) | 用户稳定特征 | 按需 |
| Fact (permanent) | 需写入身份的永驻事实 | 进入待合并队列 |
| Event | 时间性事件记录 | 按需 |
| Relation | 事件间关系 | 按需 |
| PendingIdentityMerge | 永驻事实待合并 | 队列处理 |

### 8.2 Bootstrap 时机

- 新会话打开时注入记忆上下文
- 可配置排除 fictional 事件
- 计划事件单独展示摘要

### 8.3 向量索引策略

```python
# 仅 important 事实和 real 世界事件写入向量
if cfg.memory.vector_enabled:
    _index_important_facts_vector(cfg, facts)  # 仅 tier=important
    _index_events_vector(cfg, events)          # 仅 world_kind=real
```

---

## 总结

如意72 的上下文工程具有以下特点：

1. **记忆三分法**：facts（事实）、events（事件）、relations（关系）独立存储
2. **事件扩展字段**：v3.0 支持 world_kind 和 temporal_kind 区分虚构与真实
3. **永驻事实队列**：permanent tier 进入 pending_identity 队列，需用户确认合并
4. **Bootstrap 机制**：会话冷启动时注入记忆上下文
5. **向量索引**：可选启用，基于 Ollama embedding 实现相似性检索
6. **闲时自动抽取**：后台进程按游标增量抽取，避免重复
7. **上下文压缩**：防止超长上下文影响模型性能