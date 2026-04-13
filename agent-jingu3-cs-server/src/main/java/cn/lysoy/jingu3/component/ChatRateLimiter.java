package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.config.Jingu3Properties;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 按客户端 IP 维度的令牌桶（Guava {@link RateLimiter}），用于聊天接口限流；缓存条目数有上限，避免内存膨胀。
 */
@Component
public class ChatRateLimiter {

    private static final int MAX_TRACKED_CLIENTS = 20_000;

    private final Jingu3Properties properties;
    private final LoadingCache<String, RateLimiter> limiters;

    public ChatRateLimiter(Jingu3Properties properties) {
        this.properties = properties;
        this.limiters = CacheBuilder.newBuilder()
                .maximumSize(MAX_TRACKED_CLIENTS)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build(CacheLoader.from(key -> RateLimiter.create(currentPermitsPerSecond())));
    }

    private double currentPermitsPerSecond() {
        return Math.max(properties.getRateLimit().getChatPermitsPerMinute() / 60.0, 1.0e-6);
    }

    /**
     * @param clientKey 通常为 {@code HttpServletRequest#getRemoteAddr()} 或 X-Forwarded-For 首段
     * @return true 表示允许通过
     */
    public boolean tryAcquire(String clientKey) {
        if (!properties.getRateLimit().isEnabled()) {
            return true;
        }
        String key = (clientKey == null || clientKey.isBlank()) ? "unknown" : clientKey;
        try {
            return limiters.get(key).tryAcquire();
        } catch (ExecutionException e) {
            return true;
        }
    }
}
