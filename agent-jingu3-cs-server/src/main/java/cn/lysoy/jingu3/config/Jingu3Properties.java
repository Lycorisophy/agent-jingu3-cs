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
}
