package cn.lysoy.jingu3.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 仅在 {@code jingu3.redis.enabled=true} 时注册连接；默认不启用，避免无 Redis 环境启动失败。
 */
@Configuration
@ConditionalOnProperty(prefix = "jingu3.redis", name = "enabled", havingValue = "true")
public class Jingu3RedisConfiguration {

    @Bean
    public LettuceConnectionFactory jingu3RedisConnectionFactory(Jingu3Properties properties) {
        Jingu3Properties.Redis r = properties.getRedis();
        RedisStandaloneConfiguration c = new RedisStandaloneConfiguration();
        c.setHostName(r.getHost());
        c.setPort(r.getPort());
        c.setDatabase(r.getDatabase());
        String pwd = r.getPassword();
        if (pwd != null && !pwd.isBlank()) {
            c.setPassword(RedisPassword.of(pwd));
        }
        return new LettuceConnectionFactory(c);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory jingu3RedisConnectionFactory) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(jingu3RedisConnectionFactory);
        return t;
    }
}
