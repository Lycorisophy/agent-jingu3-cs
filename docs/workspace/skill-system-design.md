# JinGu3 技能（Skill）系统设计

> **说明**：技能系统与工作空间分立。工作空间（隔离目录、安全文件、`WorkspaceFileService`、沙箱）见 [workspace-design.md](./workspace-design.md)。路线图与版本归属见 [开发路线图 v0.7](../计划/开发路线图.md#v07--技能工具系统扩展原史诗③顺延--工作空间workspace)。

## 1. 设计理念

### 1.1 参考实现

本设计参考 **MiniMax Agent 技能系统**，采用 C/S 架构：

| 组件 | 职责 |
|------|------|
| **服务端** | 存储技能元数据、用户技能关联，提供技能下载 |
| **客户端** | 下载技能、执行技能、管理本地技能缓存 |
| **对象存储** | 存储技能原文件和脚本（MinIO/OSS） |

### 1.2 核心原则

1. **技能渐进式披露（Skill Progressive Disclosure）**
   - 技能元信息始终在上下文中
   - Body 部分仅在技能触发时才加载
   - 单个 SKILL.md 的 Body 应小于 5K
   - 其他资源（脚本、数据）按需加载

2. **云端技能不可修改**
   - 从服务端下载的技能为只读
   - 用户可自定义 **外挂技能**，存放在用户指定目录
   - 外挂技能优先级高于云端技能

3. **客户端主导执行**
   - 技能由客户端执行，服务端仅提供技能信息
   - 客户端向服务端上报可用技能列表
   - 服务端根据可用技能进行路由

---

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              服务端 (JinGu3 Server)                     │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────────┐  │
│  │ Skill API   │    │ User Skill  │    │    Skill Metadata Service   │  │
│  │ - 列表      │    │  - 关联管理 │    │    - CRUD 技能信息          │  │
│  │ - 下载URL   │    │  - 权限控制 │    │    - 版本管理               │  │
│  │ - 搜索      │    │  - 同步状态 │    │    - 分类/标签              │  │
│  └─────────────┘    └─────────────┘    └─────────────────────────────┘  │
│         │                  │                         │                  │
│         └──────────────────┼─────────────────────────┘                  │
│                            ▼                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                         MySQL 数据库                               │ │
│  │  ┌────────────┐  ┌──────────────┐  ┌────────────────────────┐     │ │
│  │  │ skill      │  │ user_skill   │  │ skill_version          │     │ │
│  │  │ 技能元数据  │  │ 用户技能关联  │  │ 技能版本历史           │     │ │
│  │  └────────────┘  └──────────────┘  └────────────────────────┘     │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                    │                                    ▲
                    │ 技能文件URL (签名URL)               │ 技能信息
                    ▼                                    │
┌─────────────────────────────────────────────────────────────────────────┐
│                         对象存储 (MinIO/OSS)                             │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │  skills/{skillId}/{version}/                                      │ │
│  │  ├── SKILL.md          # 技能定义（必需）                          │ │
│  │  ├── metadata.yaml     # 元数据                                   │ │
│  │  ├── scripts/          # 脚本文件                                 │ │
│  │  └── assets/           # 资源文件                                  │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                    │
                    │ 下载技能包
                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              客户端 (JinGu3 Client)                     │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                     Skill Manager                                │    │
│  │  - 下载/同步云端技能                                             │    │
│  │  - 管理本地技能缓存                                              │    │
│  │  - 技能执行引擎                                                  │    │
│  │  - 向服务端上报可用技能                                          │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│         │                           │                    │              │
│         ▼                           ▼                    ▼              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    │
│  │ 本地技能目录     │    │  技能执行沙箱   │    │  技能执行器     │    │
│  │ ~/.jingu3/      │    │  (代码安全执行) │    │  工具注册       │    │
│  │ skills/         │    │                 │    │                 │    │
│  │ ├── cloud/      │    │                 │    │                 │    │
│  │ │  └── {skill}  │    │                 │    │                 │    │
│  │ └── external/   │    │                 │    │                 │    │
│  │     └── {skill} │    │                 │    │                 │    │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 技能渐进式披露（Skill Progressive Disclosure）

### 3.1 核心概念

**渐进式披露** 是一种优化 LLM 上下文的技术：

```
┌─────────────────────────────────────────────────────────────┐
│                    上下文构成（始终加载）                     │
├─────────────────────────────────────────────────────────────┤
│  1. 系统提示词                                                │
│  2. 当前对话历史                                              │
│  3. 技能元信息（轻量，始终可见）                              │
│     └── name, description, trigger_words, tags              │
├─────────────────────────────────────────────────────────────┤
│                    Body 部分（按需加载）                     │
├─────────────────────────────────────────────────────────────┤
│  4. 技能详情 Body（仅当技能被触发时）                         │
│     └── 详细指令、脚本路径、示例等                            │
│  5. 脚本/资源文件（仅当技能需要时）                           │
│     └── Python/JS 脚本、模板文件等                           │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 SKILL.md 结构

```yaml
---
# === 元信息（始终在上下文中，JSON 格式）===
name: minimax-pdf
description: 专业PDF文档创建工具，支持创建、填充、重构三种模式
version: 1.2.0
category: document
tags: [pdf, document, 生成, 报告]
trigger_words: [pdf, PDF, 生成文档, 创建PDF]
icon: assets/icon.png
entry: scripts/main.js

# === Body 部分（按需加载，应小于 5K）===
---

# MiniMax PDF 技能

## 功能说明
本技能用于创建专业PDF文档，支持以下功能：
1. **创建模式**：从零开始创建 PDF
2. **填充模式**：填充已有 PDF 模板
3. **重构模式**：修改现有 PDF 内容

## 使用方法
当用户提到以下关键词时触发：
- "生成 PDF"
- "创建文档"
- "导出为 PDF"

## 执行流程
1. 调用 `scripts/render_cover.js` 渲染封面
2. 调用 `scripts/render_page.js` 渲染内容页
3. 使用 `scripts/assemble.js` 合并为 PDF

## 示例
用户说：「生成一份项目报告 PDF」
- 触发本技能
- 加载 scripts/ 目录下的脚本
- 执行 PDF 生成流程
```

### 3.3 元信息字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | string | 是 | 技能名称 |
| `description` | string | 是 | 简短描述（<200字符） |
| `version` | string | 是 | 语义化版本 |
| `category` | string | 否 | 分类：document, code, design 等 |
| `tags` | array | 否 | 标签列表 |
| `trigger_words` | array | 是 | 触发词列表 |
| `icon` | string | 否 | 图标路径 |
| `entry` | string | 否 | 入口脚本路径 |

### 3.4 客户端加载策略

```java
// SkillLoader.java
public class SkillLoader {

    /**
     * 加载技能元信息（轻量，始终加载）
     */
    public List<SkillMetadata> loadAllMetadata(String userId) {
        // 1. 加载云端技能元信息
        List<SkillMetadata> cloudSkills = skillApi.getUserSkills(userId);
        
        // 2. 扫描本地外挂技能
        List<SkillMetadata> externalSkills = scanExternalSkills(userId);
        
        // 3. 合并（外挂优先）
        return mergeSkills(cloudSkills, externalSkills);
    }

    /**
     * 按需加载技能详情（Body + 脚本）
     * 仅当技能被触发时调用
     */
    public LoadedSkill loadSkillDetail(String skillId) {
        SkillMetadata meta = getMetadata(skillId);
        
        // 读取 Body 部分
        String body = readSkillFile(skillId, "SKILL.md");
        
        // 按需加载脚本
        Map<String, byte[]> scripts = loadScripts(meta.getEntry());
        
        return new LoadedSkill(meta, body, scripts);
    }
}
```

---

## 4. 云端技能 vs 外挂技能

### 4.1 对比

| 特性 | 云端技能 | 外挂技能 |
|------|----------|----------|
| **来源** | 服务端下载 | 用户本地目录 |
| **存储位置** | `~/.jingu3/skills/cloud/` | `~/.jingu3/skills/external/` |
| **可修改** | ❌ 不可修改（只读） | ✅ 可自由修改 |
| **更新** | 服务端推送更新 | 手动更新 |
| **优先级** | 低 | 高（覆盖同名云端技能） |
| **元数据** | 服务端管理 | 本地解析 SKILL.md |

### 4.2 云端技能保护机制

```java
// CloudSkillProtection.java
public class CloudSkillProtection {

    /**
     * 验证技能文件是否可写
     */
    public boolean isWritable(String skillPath) {
        // 云端技能目录始终只读
        if (skillPath.startsWith(CLOUD_SKILLS_DIR)) {
            return false;
        }
        // 仅外挂技能可写
        return skillPath.startsWith(EXTERNAL_SKILLS_DIR);
    }

    /**
     * 禁止删除云端技能文件
     */
    public void protectDelete(String skillPath) {
        if (skillPath.startsWith(CLOUD_SKILLS_DIR)) {
            throw new SecurityException("云端技能不可删除，请使用「取消订阅」代替");
        }
    }
}
```

### 4.3 本地技能目录结构

```
~/.jingu3/skills/
├── cloud/                        # 云端技能（只读）
│   ├── minimax-pdf/
│   │   ├── v1.2.0/
│   │   │   ├── SKILL.md
│   │   │   ├── metadata.yaml
│   │   │   ├── scripts/
│   │   │   │   ├── render_cover.js
│   │   │   │   └── render_page.js
│   │   │   └── assets/
│   │   └── v1.3.0/              # 新版本
│   └── minimax-xlsx/
│       └── v1.0.0/
│
└── external/                     # 外挂技能（可读写）
    ├── my-custom-pdf/           # 用户自定义 PDF 技能
    │   ├── SKILL.md
    │   └── scripts/
    └── company-template/         # 公司模板技能
        └── SKILL.md
```

---

## 5. 客户端-服务端通信

### 5.1 客户端上报可用技能

客户端执行技能前，需要告知服务端当前可用的技能列表：

```json
// POST /api/v1/users/{userId}/skills/available
{
    "skills": [
        {
            "skillId": "minimax-pdf",
            "version": "1.2.0",
            "isExternal": false,
            "triggerWords": ["pdf", "PDF", "生成文档"]
        },
        {
            "skillId": "my-custom-pdf",
            "version": "1.0.0",
            "isExternal": true,
            "externalPath": "~/.jingu3/skills/external/my-custom-pdf",
            "triggerWords": ["自定义PDF"]
        }
    ]
}
```

### 5.2 服务端返回技能配置

```json
// 服务端根据可用技能返回路由配置
{
    "enabledSkills": ["minimax-pdf", "my-custom-pdf"],
    "skillConfigs": {
        "minimax-pdf": {
            "type": "cloud",
            "permissions": ["read", "write"],
            "allowedResources": ["workspace"]
        },
        "my-custom-pdf": {
            "type": "external", 
            "permissions": ["read", "write", "execute"],
            "allowedResources": ["workspace", "network"]
        }
    }
}
```

### 5.3 技能执行流程

```
┌─────────────────────────────────────────────────────────────────┐
│                     技能执行完整流程                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. 用户请求 → 客户端                                            │
│     「生成一份 PDF 报告」                                        │
│         │                                                        │
│         ▼                                                        │
│  2. 客户端扫描可用技能                                          │
│     - 扫描本地 cloud/ 目录                                      │
│     - 扫描本地 external/ 目录                                   │
│     - 匹配触发词 "PDF" → 找到 minimax-pdf                       │
│         │                                                        │
│         ▼                                                        │
│  3. 客户端上报可用技能 → 服务端                                 │
│     POST /api/v1/users/{userId}/skills/available                │
│         │                                                        │
│         ▼                                                        │
│  4. 服务端返回技能配置                                          │
│     { "enabledSkills": ["minimax-pdf", ...] }                   │
│         │                                                        │
│         ▼                                                        │
│  5. 客户端执行技能                                              │
│     - 加载 SKILL.md 元信息                                       │
│     - 加载 scripts/ 脚本                                        │
│     - 在沙箱中执行                                               │
│         │                                                        │
│         ▼                                                        │
│  6. 客户端上报结果 → 服务端                                     │
│     POST /api/v1/users/{userId}/skills/result                   │
│     { "skillId": "minimax-pdf", "status": "success" }          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. 数据库设计

### 6.1 技能元数据表

```sql
CREATE TABLE skill (
    id              VARCHAR(36) PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    slug            VARCHAR(128) UNIQUE NOT NULL,        -- URL 友好名称
    description     TEXT,                                  -- 技能描述（<200字符）
    version         VARCHAR(32) NOT NULL,                 -- 当前版本
    category        VARCHAR(64),                          -- 分类
    tags            JSON,                                  -- 标签列表
    trigger_words   JSON,                                 -- 触发词列表
    icon_url        VARCHAR(512),                         -- 图标 URL
    storage_path    VARCHAR(256) NOT NULL,               -- OSS 存储路径
    file_size       BIGINT,                               -- 文件大小
    checksum        VARCHAR(64),                         -- MD5 校验
    author_id       VARCHAR(64),                          -- 作者 ID
    is_public       BOOLEAN DEFAULT TRUE,                -- 是否公开
    is_official     BOOLEAN DEFAULT FALSE,                -- 是否官方
    status          VARCHAR(20) DEFAULT 'ACTIVE',       -- ACTIVE, DEPRECATED, REMOVED
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_category (category),
    INDEX idx_is_public (is_public),
    INDEX idx_is_official (is_official)
);
```

### 6.2 技能版本表

```sql
CREATE TABLE skill_version (
    id              VARCHAR(36) PRIMARY KEY,
    skill_id        VARCHAR(36) NOT NULL,
    version         VARCHAR(32) NOT NULL,
    storage_path    VARCHAR(256) NOT NULL,               -- OSS 存储路径
    file_size       BIGINT,
    checksum        VARCHAR(64),
    changelog       TEXT,                                 -- 更新日志
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (skill_id) REFERENCES skill(id),
    UNIQUE KEY uk_skill_version (skill_id, version)
);
```

### 6.3 用户技能关联表

```sql
CREATE TABLE user_skill (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(64) NOT NULL,
    skill_id        VARCHAR(36) NOT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',       -- ACTIVE, DISABLED, SYNCING
    local_version   VARCHAR(32),                         -- 本地版本
    server_version  VARCHAR(32),                        -- 服务端版本
    is_external     BOOLEAN DEFAULT FALSE,              -- 是否外挂技能
    external_path   VARCHAR(512),                        -- 外挂路径（仅 is_external=true）
    last_sync_at    TIMESTAMP,                          -- 最后同步时间
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (skill_id) REFERENCES skill(id),
    UNIQUE KEY uk_user_skill (user_id, skill_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);
```

---

## 7. API 设计

### 7.1 服务端 API

```
=== 技能市场 ===
GET    /api/v1/skills                          # 获取公开技能列表
GET    /api/v1/skills/{skillId}                # 获取技能详情
GET    /api/v1/skills/search?q=xxx             # 搜索技能

=== 用户技能管理 ===
GET    /api/v1/users/{userId}/skills            # 获取用户已订阅技能
POST   /api/v1/users/{userId}/skills            # 订阅技能
DELETE /api/v1/users/{userId}/skills/{skillId}  # 取消订阅
POST   /api/v1/users/{userId}/skills/sync       # 同步技能版本

=== 技能下载 ===
GET    /api/v1/skills/{skillId}/download       # 获取下载 URL（签名 URL）
GET    /api/v1/skills/{skillId}/versions       # 获取版本列表

=== 技能结果上报 ===
POST   /api/v1/users/{userId}/skills/result     # 上报技能执行结果
```

### 7.2 客户端上报 API

```
=== 客户端 → 服务端 ===
POST   /api/v1/users/{userId}/skills/available  # 上报可用技能（执行前）
POST   /api/v1/users/{userId}/skills/usage      # 上报技能使用统计
```

### 7.3 请求/响应示例

```json
// GET /api/v1/skills/{skillId}
{
    "id": "minimax-pdf",
    "name": "MiniMax PDF",
    "slug": "minimax-pdf",
    "description": "专业PDF文档创建工具",
    "version": "1.2.0",
    "category": "document",
    "tags": ["pdf", "document"],
    "triggerWords": ["pdf", "PDF", "生成文档"],
    "iconUrl": "https://cdn.example.com/skills/minimax-pdf/icon.png",
    "downloadUrl": "https://storage.example.com/skills/minimax-pdf/v1.2.0.zip",
    "author": {
        "id": "minimax-official",
        "name": "MiniMax Official"
    },
    "isOfficial": true,
    "usageCount": 1870
}

// POST /api/v1/users/{userId}/skills/available
// Request:
{
    "skills": [
        {
            "skillId": "minimax-pdf",
            "version": "1.2.0",
            "isExternal": false
        },
        {
            "skillId": "my-custom-skill",
            "version": "1.0.0",
            "isExternal": true,
            "externalPath": "/Users/xxx/.jingu3/skills/external/my-custom-skill"
        }
    ]
}

// Response:
{
    "enabledSkills": ["minimax-pdf", "my-custom-skill"],
    "updates": [
        {
            "skillId": "minimax-pdf",
            "newVersion": "1.3.0",
            "updateAvailable": true
        }
    ]
}
```

---

## 8. 技能文件结构

### 8.1 存储在 OSS 的技能包

```
skills/
└── minimax-pdf/
    ├── v1.2.0/
    │   ├── SKILL.md               # 技能定义（必需）
    │   ├── metadata.yaml          # 扩展元数据
    │   ├── scripts/
    │   │   ├── render_cover.js
    │   │   ├── render_page.js
    │   │   └── assemble.js
    │   └── assets/
    │       ├── template.html
    │       └── styles.css
    └── v1.3.0/
        ├── SKILL.md
        ├── scripts/
        └── assets/
```

### 8.2 SKILL.md 完整示例

```yaml
---
name: minimax-pdf
description: 专业PDF文档创建工具，支持创建、填充、重构三种模式，15种文档类型
version: 1.3.0
category: document
tags: [pdf, document, 生成, 报告]
trigger_words:
  - pdf
  - PDF
  - 生成文档
  - 创建PDF
  - 导出PDF
icon: assets/icon.png
entry: scripts/main.js
---

# MiniMax PDF 技能

## 功能说明
本技能用于创建专业PDF文档，支持三种模式：

### 1. 创建模式
从零开始创建 PDF 文档

### 2. 填充模式
填充已有 PDF 模板

### 3. 重构模式
修改现有 PDF 内容

## 触发条件
当用户提到以下关键词时触发：
- "生成 PDF"、"创建文档"、"导出 PDF"

## 执行流程
1. 解析用户需求
2. 调用 `scripts/render_cover.js` 渲染封面
3. 调用 `scripts/render_page.js` 渲染内容页
4. 使用 `scripts/assemble.js` 合并为 PDF

## 注意事项
- 仅支持生成 PDF 格式
- 最大支持 100 页
- 图片需小于 5MB
```

---

## 9. 实现优先级

### Phase 1: 基础技能系统（1周）
- [ ] 数据库表创建
- [ ] 技能元数据 CRUD API
- [ ] 用户技能关联管理
- [ ] OSS 技能文件存储

### Phase 2: 客户端技能管理（1周）
- [ ] SkillManager 核心实现
- [ ] 云端技能下载与同步
- [ ] 本地外挂技能扫描
- [ ] 技能元信息加载（渐进式）

### Phase 3: 技能执行（1周）
- [ ] 技能执行引擎
- [ ] 脚本加载与执行
- [ ] 客户端-服务端通信
- [ ] 执行结果上报

### Phase 4: 高级功能（1周）
- [ ] 技能版本管理
- [ ] 技能更新推送
- [ ] 技能使用统计
- [ ] 技能市场功能

---

## 10. 配置文件

```yaml
# application.yml
jingu3:
  skill:
    # 存储配置
    storage:
      type: minio                              # minio / oss / s3
      endpoint: http://localhost:9000
      bucket: skills
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
    
    # 下载配置
    download:
      url-expiry-seconds: 3600                 # 签名 URL 过期时间
      max-file-size-mb: 100                    # 最大技能包大小
    
    # 客户端配置
    client:
      skills-dir: ~/.jingu3/skills              # 本地技能目录
      cloud-dir: cloud                         # 云端技能子目录
      external-dir: external                   # 外挂技能子目录
      auto-sync: true                          # 启动时自动同步
      sync-interval-hours: 24                  # 同步间隔
```

---

## 11. 安全性考虑

### 11.1 云端技能保护
- 云端技能存放在只读目录
- 禁止直接修改云端技能文件
- 修改需通过「取消订阅 + 重新下载」

### 11.2 外挂技能安全
- 外挂技能存放在独立目录
- 执行前进行安全扫描
- 可配置是否允许执行外部脚本

### 11.3 传输安全
- 技能下载使用签名 URL
- 校验文件 MD5/SHA256
- HTTPS 传输

