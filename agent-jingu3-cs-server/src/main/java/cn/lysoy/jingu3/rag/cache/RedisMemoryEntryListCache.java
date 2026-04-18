package cn.lysoy.jingu3.rag.cache;

import cn.lysoy.jingu3.common.vo.MemoryEntryVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 记忆列表 JSON 缓存；key 含 userId 与 maxListSize 避免配置变更脏读。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "jingu3.redis", name = "enabled", havingValue = "true")
public class RedisMemoryEntryListCache implements MemoryEntryListCache {

    private static final String KEY_PREFIX = "jingu3:mem:list:v1:";

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final Jingu3Properties properties;

    public RedisMemoryEntryListCache(
            StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper, Jingu3Properties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    private static String key(String userId, int maxListSize) {
        return KEY_PREFIX + userId + ":" + maxListSize;
    }

    @Override
    public Optional<List<MemoryEntryVo>> get(String userId, int maxListSize) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key(userId, maxListSize));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            List<MemoryEntryVo> list =
                    objectMapper.readValue(json, new TypeReference<>() {});
            return Optional.of(list);
        } catch (Exception ex) {
            log.warn("读取记忆列表缓存失败 userId={}", userId, ex);
            return Optional.empty();
        }
    }

    @Override
    public void put(String userId, int maxListSize, List<MemoryEntryVo> list) {
        try {
            String json = objectMapper.writeValueAsString(list == null ? Collections.emptyList() : list);
            int ttl = Math.max(1, properties.getRedis().getListTtlSeconds());
            stringRedisTemplate
                    .opsForValue()
                    .set(key(userId, maxListSize), json, Duration.ofSeconds(ttl));
        } catch (Exception ex) {
            log.warn("写入记忆列表缓存失败 userId={}", userId, ex);
        }
    }

    @Override
    public void evictListForUser(String userId) {
        try {
            Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ex) {
            log.warn("清除记忆列表缓存失败 userId={}", userId, ex);
        }
    }
}
