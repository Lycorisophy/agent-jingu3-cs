# JinGu3 工作空间系统设计

## 1. 概述

### 1.1 什么是工作空间

**工作空间（Workspace）** 是 JinGu3 AI Agent 系统的核心能力组件：

| 角色 | 说明 |
|------|------|
| 代码执行沙箱 | 安全执行 AI 生成的代码，隔离危险操作 |
| 文件操作空间 | AI 操作项目文件的隔离目录，防止越权访问 |

### 1.2 设计目标

- **安全隔离**：AI 操作限制在工作空间目录内，禁止访问外部系统文件
- **可控执行**：代码执行在沙箱内，支持超时、资源限制
- **透明可控**：用户可查看、清空、重置工作空间
- **工具集成**：作为内置工具注册到 ToolRegistry

---

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        AI Agent Core                            │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐  │
│  │ Plan Mode   │  │ ReAct Mode   │  │ Workflow Mode          │  │
│  └──────┬──────┘  └──────┬───────┘  └───────────┬────────────┘  │
│         │               │                      │                │
│         └───────────────┼──────────────────────┘                │
│                         ▼                                       │
│              ┌──────────────────┐                               │
│              │   ToolRegistry   │                               │
│              └────────┬─────────┘                               │
│                       │                                          │
└───────────────────────┼────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌───────────────┐ ┌─────────────┐ ┌──────────────┐
│ FileOperation │ │ CodeSandbox │ │ Other Tools  │
│    Tools      │ │             │ │              │
└───────┬───────┘ └──────┬──────┘ └──────────────┘
        │                │
        ▼                ▼
┌───────────────────────────────────┐
│        Workspace System           │
│  ┌─────────────────────────────┐  │
│  │     WorkspaceManager         │  │
│  │  - 创建/删除/切换工作空间   │  │
│  └─────────────┬───────────────┘  │
│                ▼                  │
│  ┌─────────────────────────────┐  │
│  │     Workspace               │  │
│  │  - rootPath: 隔离根目录     │  │
│  │  - userId: 所属用户         │  │
│  │  - createdAt: 创建时间       │  │
│  └─────────────┬───────────────┘  │
│                ▼                  │
│  ┌─────────────────────────────┐  │
│  │   WorkspaceFileService      │  │
│  │  - 安全文件读写操作         │  │
│  │  - 路径验证（防越界）       │  │
│  └─────────────┬───────────────┘  │
│                ▼                  │
│  ┌─────────────────────────────┐  │
│  │   SandboxExecutor           │  │
│  │  - 代码执行引擎             │  │
│  │  - 超时/资源限制           │  │
│  │  - 进程隔离               │  │
│  └─────────────────────────────┘  │
└───────────────────────────────────┘
```

---

## 3. 核心组件设计

### 3.1 实体类

```java
// src/main/java/cn/lysoy/jingu3/workspace/entity/Workspace.java
package cn.lysoy.jingu3.workspace.entity;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工作空间实体
 */
@Data
@Builder
public class Workspace {
    /** 工作空间唯一ID */
    private String id;

    /** 所属用户ID */
    private String userId;

    /** 工作空间根目录路径 */
    private String rootPath;

    /** 工作空间名称 */
    private String name;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveAt;

    /** 状态: ACTIVE, ARCHIVED */
    private String status;

    /** 存储配额(MB) */
    private Long quotaMb;
}
```

### 3.2 WorkspaceManager - 工作空间管理器

```java
// src/main/java/cn/lysoy/jingu3/workspace/WorkspaceManager.java
package cn.lysoy.jingu3.workspace;

import cn.lysoy.jingu3.workspace.entity.Workspace;
import java.util.Optional;

/**
 * 工作空间管理器接口
 */
public interface WorkspaceManager {

    /**
     * 为用户获取当前工作空间（自动创建不存在的）
     */
    Workspace getOrCreateWorkspace(String userId);

    /**
     * 获取用户的工作空间（可能为空）
     */
    Optional<Workspace> getWorkspace(String userId);

    /**
     * 重置工作空间（清空所有文件）
     */
    void resetWorkspace(String userId);

    /**
     * 删除工作空间
     */
    void deleteWorkspace(String userId);

    /**
     * 获取工作空间使用统计
     */
    WorkspaceStats getStats(String userId);

    /**
     * 工作空间统计信息
     */
    record WorkspaceStats(long fileCount, long totalSizeBytes, long quotaMb) {}
}
```

### 3.3 WorkspaceFileService - 安全文件操作服务

```java
// src/main/java/cn/lysoy/jingu3/workspace/WorkspaceFileService.java
package cn.lysoy.jingu3.workspace;

import java.nio.file.Path;
import java.util.List;

/**
 * 工作空间文件操作服务（安全版本）
 */
public interface WorkspaceFileService {

    /**
     * 安全读取文件 - 验证路径在工作空间内
     */
    String readFile(String userId, String relativePath);

    /**
     * 安全写入文件
     */
    void writeFile(String userId, String relativePath, String content);

    /**
     * 安全删除文件
     */
    void deleteFile(String userId, String relativePath);

    /**
     * 列出目录内容
     */
    List<FileInfo> listDirectory(String userId, String relativePath);

    /**
     * 创建目录
     */
    void createDirectory(String userId, String relativePath);

    /**
     * 文件信息
     */
    record FileInfo(String name, boolean isDirectory, long size, String lastModified) {}

    /**
     * 验证路径是否在工作空间内（防越界）
     */
    boolean isPathSafe(String userId, String relativePath);

    /**
     * 获取文件树结构
     */
    FileTree getFileTree(String userId, String relativePath, int maxDepth);

    /**
     * 文件树节点
     */
    record FileTree(String name, boolean isDirectory, List<FileTree> children) {}
}
```

### 3.4 SandboxExecutor - 代码执行沙箱

```java
// src/main/java/cn/lysoy/jingu3/workspace/sandbox/SandboxExecutor.java
package cn.lysoy.jingu3.workspace.sandbox;

import cn.lysoy.jingu3.workspace.sandbox.SandboxResult;

/**
 * 代码执行沙箱接口
 */
public interface SandboxExecutor {

    /**
     * 执行代码（支持多种语言）
     * @param language 语言类型: python, javascript, bash, java
     * @param code 代码内容
     * @param timeoutSeconds 超时秒数
     */
    SandboxResult execute(String userId, String language, String code, int timeoutSeconds);

    /**
     * 执行文件中的代码
     */
    SandboxResult executeFile(String userId, String filePath, int timeoutSeconds);
}
```

```java
// src/main/java/cn/lysoy/jingu3/workspace/sandbox/SandboxResult.java
package cn.lysoy.jingu3.workspace.sandbox;

import lombok.Builder;
import lombok.Data;

/**
 * 沙箱执行结果
 */
@Data
@Builder
public class SandboxResult {
    /** 是否成功 */
    private boolean success;

    /** 标准输出 */
    private String stdout;

    /** 标准错误 */
    private String stderr;

    /** 退出码 */
    private int exitCode;

    /** 执行耗时(ms) */
    private long executionTimeMs;

    /** 错误类型 */
    private String errorType;

    /** 是否超时 */
    private boolean timeout;
}
```

---

## 4. 工具实现

### 4.1 内置工具注册

```java
// src/main/java/cn/lysoy/jingu3/workspace/tools/WorkspaceToolProvider.java
package cn.lysoy.jingu3.workspace.tools;

import cn.lysoy.jingu3.tool.Jingu3Tool;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 工作空间工具提供者
 * 自动注册所有工作空间相关工具到 ToolRegistry
 */
@Component
public class WorkspaceToolProvider {

    private final WorkspaceFileService fileService;
    private final SandboxExecutor sandboxExecutor;

    public List<Jingu3Tool> getTools() {
        return List.of(
            new ReadFileTool(fileService),
            new WriteFileTool(fileService),
            new ListFilesTool(fileService),
            new ReadMultipleFilesTool(fileService),
            new WriteMultipleFilesTool(fileService),
            new ExecuteCodeTool(sandboxExecutor),
            new GlobTool(fileService),
            new GrepTool(fileService),
            new BashTool(sandboxExecutor)
        );
    }
}
```

### 4.2 核心工具实现

```java
// ReadFileTool.java
public class ReadFileTool implements Jingu3Tool {

    private final WorkspaceFileService fileService;

    @Override
    public String id() {
        return "read_file";
    }

    @Override
    public String description() {
        return "read_file(path): 读取工作空间中指定文件的内容。path 为相对于工作空间的路径，如 'src/main.java'。";
    }

    @Override
    public String execute(String input) {
        // input 是 JSON: {"userId": "xxx", "path": "xxx"}
        // 或直接是路径字符串
        // 安全验证后读取
    }
}

// WriteFileTool.java
public class WriteFileTool implements Jingu3Tool {
    @Override
    public String id() { return "write_file"; }
    @Override
    public String description() {
        return "write_file(path, content): 写入内容到工作空间的文件。path 为相对路径，content 为文件内容。";
    }
    // ...
}

// ExecuteCodeTool.java
public class ExecuteCodeTool implements Jingu3Tool {
    @Override
    public String id() { return "execute_code"; }
    @Override
    public String description() {
        return "execute_code(language, code): 在沙箱中安全执行代码。language 支持 python/javascript/bash，返回执行结果。";
    }
    // ...
}
```

---

## 5. 安全机制

### 5.1 路径安全验证

```java
// src/main/java/cn/lysoy/jingu3/workspace/security/PathValidator.java
package cn.lysoy.jingu3.workspace.security;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径安全验证器 - 防止目录遍历攻击
 */
public class PathValidator {

    /**
     * 验证路径是否在工作空间根目录内
     */
    public static boolean isPathSafe(String workspaceRoot, String relativePath) {
        try {
            Path root = Paths.get(workspaceRoot).toRealPath();
            Path target = root.resolve(relativePath).normalize().toRealPath();
            return target.starts(root);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证路径是否在允许列表中
     */
    public static boolean isAllowedExtension(String path, List<String> allowedExtensions) {
        String ext = getExtension(path);
        return allowedExtensions.contains(ext);
    }

    private static String getExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot + 1) : "";
    }
}
```

### 5.2 沙箱安全配置

```yaml
# application.yml
jingu3:
  workspace:
    # 工作空间根目录
    root-dir: /data/workspaces
    # 默认配额(MB)
    default-quota-mb: 1024
    # 单文件最大限制(MB)
    max-file-size-mb: 100

  sandbox:
    # 执行超时(秒)
    default-timeout: 30
    max-timeout: 300
    # 内存限制(MB)
    memory-limit-mb: 512
    # 禁止的命令
    forbidden-commands:
      - rm -rf /
      - :(){ :|:& };:
      - chmod -R 777
      - wget/curl 危险URL
```

### 5.3 禁止的危险操作

| 类别 | 禁止操作 |
|------|----------|
| 文件系统 | `rm -rf /`, `chmod 777`, 符号链接到外部 |
| 网络 | 访问内网IP, 发起攻击流量 |
| 进程 | Fork炸弹, 后台驻留进程 |
| 敏感文件 | `/etc/passwd`, `~/.ssh/`, `/proc/` |

---

## 6. API 设计

### 6.1 工作空间管理 API

```
POST   /api/workspace              - 创建工作空间
GET    /api/workspace              - 获取当前用户工作空间
DELETE /api/workspace              - 删除工作空间
POST   /api/workspace/reset        - 重置工作空间
GET    /api/workspace/stats        - 获取使用统计
```

### 6.2 文件操作 API

```
GET    /api/workspace/files?path=xxx      - 列出目录
GET    /api/workspace/files/read?path=xxx  - 读取文件
PUT    /api/workspace/files/write         - 写入文件
POST   /api/workspace/files/mkdir         - 创建目录
DELETE /api/workspace/files?path=xxx       - 删除文件
```

### 6.3 代码执行 API

```
POST   /api/workspace/execute            - 执行代码
POST   /api/workspace/execute/file       - 执行文件
GET    /api/workspace/execute/history     - 执行历史
```

---

## 7. 数据库设计

```sql
-- 工作空间表
CREATE TABLE workspace (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(64) NOT NULL,
    root_path   VARCHAR(512) NOT NULL,
    name        VARCHAR(128),
    quota_mb    BIGINT DEFAULT 1024,
file_count  BIGINT DEFAULT 0,
    total_size  BIGINT DEFAULT 0,
    status      VARCHAR(20) DEFAULT 'ACTIVE',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
);

-- 执行历史表
CREATE TABLE execution_history (
    id          VARCHAR(36) PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL,
    language    VARCHAR(32) NOT NULL,
    code_hash   VARCHAR(64),
    stdout      TEXT,
    stderr      TEXT,
    exit_code   INT,
    duration_ms BIGINT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (workspace_id) REFERENCES workspace(id)
);
```

---

## 8. 实现阶段

### Phase 1: 基础文件操作（1周）
- [ ] WorkspaceManager 核心实现
- [ ] WorkspaceFileService 文件读写
- [ ] 路径安全验证
- [ ] 基础工具注册

### Phase 2: 代码执行沙箱（1周）
- [ ] SandboxExecutor 接口定义
- [ ] Python/JS 执行支持
- [ ] 超时与资源限制
- [ ] ExecuteCodeTool 实现

### Phase 3: 高级功能（1周）
- [ ] 执行历史记录
- [ ] 存储配额管理
- [ ] 多语言支持扩展
- [ ] API 完善

---

## 9. 与现有系统集成

```
┌─────────────────────────────────────────────────────────────┐
│                     现有 JinGu3 系统                        │
├─────────────────────────────────────────────────────────────┤
│  ToolRegistry ←── WorkspaceToolProvider (自动注册)          │
│                                                             │
│  ReAct Mode ←──→ ToolStepService ←──→ Workspace Tools       │
│                    ↓                                        │
│              WorkspaceManager                               │
│                    ↓                                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Workspace System                        │   │
│  │  WorkspaceFileService │ SandboxExecutor              │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 关键集成点

1. **ToolRegistry 自动发现**：通过 `@Component` 注解自动注册工具
2. **与 ReAct 模式集成**：ToolStepService 调用 Workspace 工具
3. **与 Plan 模式集成**：子任务可使用文件操作工具
4. **配置复用**：复用 Jingu3Properties 配置体系
