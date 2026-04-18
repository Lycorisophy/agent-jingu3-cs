package cn.lysoy.jingu3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 与 {@code jingu3.*} 配置项绑定，避免魔法数字散落在 Controller/切面中。
 */
@Data
@ConfigurationProperties(prefix = "jingu3")
public class Jingu3Properties {

    private Http http = new Http();

    private RateLimit rateLimit = new RateLimit();

    private Snowflake snowflake = new Snowflake();

    private Tool tool = new Tool();

    private Routing routing = new Routing();

    private Cron cron = new Cron();

    private Workspace workspace = new Workspace();

    private Memory memory = new Memory();

    /** v0.7：技能市场元数据（{@code skill} 表） */
    private Skill skill = new Skill();

    /** 本地/自建 Redis（缓存）；未接业务 Bean 前仅作配置占位，默认本机 6379 */
    private Redis redis = new Redis();

    /** 本地/自建 Milvus（向量库）；未接向量检索前仅作配置占位，默认 gRPC 19530 */
    private Milvus milvus = new Milvus();

    /** Elasticsearch 客户端占位；事件检索已迁 MySQL+Milvus，不再使用 ES 事件索引 */
    private Elasticsearch elasticsearch = new Elasticsearch();

    private Ollama ollama = new Ollama();

    /** 对话侧行为（用户提示词落库等） */
    private Chat chat = new Chat();

    /** 事件域：检索 topK、关系扩展上限等（MySQL + Milvus） */
    private Events events = new Events();

    /** 对称加密密钥（用户提示词密文） */
    private Crypto crypto = new Crypto();

    @Data
    public static class Chat {

        /**
         * 是否将原始用户输入以 AES-256-GCM 加密写入 {@code user_prompt_cipher}；需配置
         * {@link Crypto#getUserPromptAesKeyBase64()}。
         */
        private boolean persistUserPrompt = false;

        /**
         * 是否启用进程内 STM（按 conversationId 保留最近若干轮 user/assistant，写入 {@link cn.lysoy.jingu3.service.guard.ExecutionContext#getHistory()}）。
         */
        private boolean stmEnabled = true;

        /** STM 保留的用户-助手轮对上限（每对两条文本）。 */
        private int stmMaxPairs = 5;

        /**
         * 是否在 STM 前注入一行 DST {@code stateJson} 摘要（需已 PATCH 过 {@code /api/v1/dst/{conversationId}}）。
         */
        private boolean stmIncludeDstSnippet = true;

        /** 注入 DST 时 {@code stateJson} 最大字符数，超出截断。 */
        private int stmMaxDstChars = 600;

        /**
         * 当请求体 {@code persistUserCorrectionAsMemory}=true 且带 {@code correctionNotes} 时，是否在对话成功后写入一条 FACT 记忆（需记忆 API 可用）。
         */
        private boolean stmPersistCorrectionMemory = false;
    }

    @Data
    public static class Crypto {

        /**
         * AES-256 密钥的 Base64（解码后须为 32 字节）；未配置时即使开启 {@link Chat#persistUserPrompt} 也不落库。
         */
        private String userPromptAesKeyBase64 = "";
    }

    @Data
    public static class Ollama {

        private String baseUrl = "http://localhost:11434";

        private String chatModel = "gnremy/qwen3.5-abliterated:35b-a3b";
    }

    @Data
    public static class Memory {

        /**
         * 是否暴露 v0.6 M1 记忆实验 API（{@code /api/v1/memory/**}）；生产可关闭至详细设计定稿。
         */
        private boolean apiEnabled = true;

        /** {@link cn.lysoy.jingu3.rag.service.DefaultMemoryService#listByUserId} 单次最大条数 */
        private int maxListSize = 100;

        /** Ollama 嵌入模型名，与路线图默认一致 */
        private String embeddingModel = "qwen3-embedding:8b";

        /**
         * 向量维度；0 表示首次嵌入时按 Ollama 返回长度自动建 Milvus 集合（需 Milvus 已启用）。
         */
        private int embeddingDimension = 0;

        /** 对话前向量检索条数上限 */
        private int retrievalTopK = 5;
    }

    @Data
    public static class Events {

        /** event_search 默认向量召回条数上限 */
        private int searchTopK = 20;

        /** 关系 1-hop 扩展时最多额外加载的邻居事件数 */
        private int relatedExpandMax = 20;
    }

    @Data
    public static class Skill {

        /**
         * 是否暴露技能市场只读 API（{@code /api/v1/skills}）；下载与写操作后续迭代。
         */
        private boolean apiEnabled = true;

        /** {@link cn.lysoy.jingu3.skill.service.DefaultSkillService#listPublicCatalog} 最大条数 */
        private int listMaxSize = 100;
    }

    @Data
    public static class Workspace {

        /** 是否注册工作空间类内置工具（read/list/write） */
        private boolean enabled = true;

        /** 工作空间物理根目录；其下按 userId 分子目录 */
        private String rootDir = System.getProperty("user.home") + "/.jingu3/workspaces";

        /** 单文件读取/写入上限（MB） */
        private long maxFileSizeMb = 10L;

        /** 配额展示与后续强校验占位（MB），Phase 3 可落库 enforcement */
        private long defaultQuotaMb = 1024L;

        /**
         * 进程沙箱（Workspace Phase 2）：默认关闭；开启后注册 {@code workspace_execute_code}。
         */
        private Sandbox sandbox = new Sandbox();

        /**
         * Phase 3：是否暴露 {@code /api/v1/workspace/**} 管理/调试 REST。
         */
        private boolean restApiEnabled = true;

        /**
         * Phase 3：沙箱执行是否写入 {@code workspace_execution}；关闭后仍保留元数据表能力。
         */
        private boolean executionHistoryEnabled = true;

        /** 执行历史 stdout/stderr 单字段落库最大字符数 */
        private int executionHistorySnippetMaxChars = 8192;

        /** REST 执行历史列表单页条数上限（客户端传入 limit 会被裁剪到此值） */
        private int executionHistoryListLimit = 50;

        @Data
        public static class Sandbox {

            /** 是否启用沙箱执行与 {@code workspace_execute_code} 工具 */
            private boolean enabled = false;

            /** 单次执行超时上限（秒），实际取 min(请求, 此值) */
            private int maxTimeoutSeconds = 30;

            /** 合并 stdout/stderr 截断长度（字符） */
            private int maxOutputChars = 65536;

            /** 提交的源码最大字符数 */
            private int maxCodeChars = 100_000;

            /** Python 可执行文件（PATH 或绝对路径） */
            private String pythonCommand = "python";

            /** Node 可执行文件（用于 javascript） */
            private String nodeCommand = "node";
        }
    }

    @Data
    public static class Cron {

        /** 与 {@code CronModeHandler} 演示文案一致 */
        private String demoSchedule = "0 0 9 * * MON-FRI";

        /** 是否轮询 {@code scheduled_task} 并在到期时触发对话 */
        private boolean schedulerEnabled = true;

        /** 轮询间隔（毫秒） */
        private long pollIntervalMs = 30_000L;
    }

    @Data
    public static class Routing {

        /**
         * 显式选择 Plan-and-Execute / Agent Team 时，是否启用意图守门（分类为 ASK/REACT 则降为 Ask）。
         * 关闭后与 v0.4 行为一致：显式 mode 直通 Handler。
         */
        private boolean explicitModeGuardEnabled = true;
    }

    @Data
    public static class Http {

        /** SSE 长连接超时（毫秒），与 {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter} 一致 */
        private long sseEmitterTimeoutMs = 600_000L;
    }

    @Data
    public static class RateLimit {

        /** 是否对 {@link cn.lysoy.jingu3.controller.app.ChatController} 启用按客户端 IP 的令牌桶限流 */
        private boolean enabled = true;

        /** 每分钟允许的聊天请求数（均分到 Guava RateLimiter 的每秒许可） */
        private double chatPermitsPerMinute = 120.0;
    }

    @Data
    public static class Snowflake {

        /** 工作机器 ID，0～31 */
        private long workerId = 1L;

        /** 数据中心 ID，0～31 */
        private long datacenterId = 1L;
    }

    @Data
    public static class Tool {

        /** 是否启用 Ask / ReAct 工具管线；关闭时行为与 v0.2 一致（无工具路由与执行） */
        private boolean enabled = true;

        /**
         * 是否暴露 {@code GET /api/v1/tools} 内置工具目录（id、description、riskLevel）；生产可关闭。
         */
        private boolean catalogApiEnabled = true;

        /**
         * 联网搜索（DuckDuckGo Instant Answer 或 Tavily）；需 {@code web-search.enabled=true} 才注册 {@code web_search}。
         */
        private WebSearch webSearch = new WebSearch();

        @Data
        public static class WebSearch {

            /** 为 true 时注册内置工具 {@code web_search}（外网 HTTP） */
            private boolean enabled = false;

            /** DuckDuckGo 无需 Key；Tavily 需配置 {@link #tavilyApiKey} */
            private WebSearchProvider provider = WebSearchProvider.DUCKDUCKGO;

            /** Tavily API Key（{@code tvly-...}）；仅 provider=TAVILY 时使用 */
            private String tavilyApiKey = "";

            /** 单次搜索 HTTP 超时（秒） */
            private int timeoutSeconds = 15;

            /** 返回条目数上限（摘要条数） */
            private int maxResults = 5;
        }
    }

    @Data
    public static class Redis {

        /** 为 true 时注册 Redis 连接与记忆列表缓存；默认 false 不连 Redis */
        private boolean enabled = false;

        private String host = "127.0.0.1";

        private int port = 6379;

        /** 无密码时留空 */
        private String password = "";

        /** 逻辑库索引，与 spring.data.redis.database 一致 */
        private int database = 0;

        /** GET /memory/entries 列表缓存 TTL（秒） */
        private int listTtlSeconds = 60;
    }

    @Data
    public static class Milvus {

        /** 为 true 时写入/检索向量；需 Milvus 可达 */
        private boolean enabled = false;

        /** 集合名（单 collection MVP） */
        private String collectionName = "jingu3_memory";

        private String host = "127.0.0.1";

        /** Milvus 2.x 默认 gRPC 端口 */
        private int port = 19530;

        /** 未启用鉴权时留空 */
        private String user = "";

        private String password = "";

        /** 事件向量独立集合（与 memory 的 {@link #collectionName} 区分） */
        private String eventsCollectionName = "jingu3_events";

        /** HNSW 索引：M（每节点最大边数） */
        private int eventsHnswM = 16;

        /** HNSW 建索引 efConstruction */
        private int eventsHnswEfConstruction = 200;

        /** 检索时 ef（越大召回/延迟越高） */
        private int eventsHnswEf = 64;
    }

    @Data
    public static class Elasticsearch {

        /** 为 true 时注册 ES 客户端（事件 API 已移除；仅保留可选扩展） */
        private boolean enabled = false;

        private String host = "127.0.0.1";

        private int port = 9200;

        private String scheme = "http";

        /** 事件索引名，与物化清单一致 */
        private String indexEvents = "jingu3-events";

        /** 未启用安全时留空 */
        private String username = "";

        private String password = "";
    }
}
