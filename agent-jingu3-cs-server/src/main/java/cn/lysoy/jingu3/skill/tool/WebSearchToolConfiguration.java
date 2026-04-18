package cn.lysoy.jingu3.skill.tool;

import cn.lysoy.jingu3.config.Jingu3Properties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 仅在 {@code jingu3.tool.enabled} 与 {@code jingu3.tool.web-search.enabled} 同时满足时注册 {@link WebSearchTool}。
 */
@Configuration
@ConditionalOnProperty(prefix = "jingu3.tool", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebSearchToolConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "jingu3.tool.web-search", name = "enabled", havingValue = "true")
    public WebSearchTool webSearchTool(Jingu3Properties properties, ObjectMapper objectMapper) {
        return new WebSearchTool(properties, objectMapper);
    }
}
