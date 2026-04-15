package cn.lysoy.jingu3.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 允许浏览器/Electron 渲染进程跨源访问 REST 与 SSE（含打包后 {@code file://} 场景下的绝对 URL 调用）。
 * <p>首版客户端见 {@code agent-jingu3-cs-client}；生产环境建议收敛为网关白名单域名。</p>
 */
@Configuration
public class Jingu3ApiCorsConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false);
    }
}
