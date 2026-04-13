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

    /** 本地/自建 Redis（缓存）；未接业务 Bean 前仅作配置占位，默认本机 6379 */
    private Redis redis = new Redis();

    /** 本地/自建 Milvus（向量库）；未接向量检索前仅作配置占位，默认 gRPC 19530 */
    private Milvus milvus = new Milvus();

    @Data
    public static class Memory {

        /**
         * 是否暴露 v0.6 M1 记忆实验 API（{@code /api/v1/memory/**}）；生产可关闭至详细设计定稿。
         */
        private boolean apiEnabled = true;

        /** {@link cn.lysoy.jingu3.memory.DefaultMemoryService#listByUserId} 单次最大条数 */
        private int maxListSize = 100;
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

        /** 是否对 {@link cn.lysoy.jingu3.controller.ChatController} 启用按客户端 IP 的令牌桶限流 */
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
    }

    @Data
    public static class Redis {

        private String host = "127.0.0.1";

        private int port = 6379;

        /** 无密码时留空 */
        private String password = "";
    }

    @Data
    public static class Milvus {

        private String host = "127.0.0.1";

        /** Milvus 2.x 默认 gRPC 端口 */
        private int port = 19530;

        /** 未启用鉴权时留空 */
        private String user = "";

        private String password = "";
    }
}
