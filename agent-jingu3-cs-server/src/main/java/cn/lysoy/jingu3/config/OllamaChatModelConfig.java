package cn.lysoy.jingu3.config;

import cn.lysoy.jingu3.common.constant.Jingu3ConfigKeys;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地 Ollama 对话模型（主推理）；配置键见 {@link Jingu3ConfigKeys#JINGU3_OLLAMA_BASE_URL} 等。
 */
@Configuration
public class OllamaChatModelConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${jingu3.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${jingu3.ollama.chat-model:gnremy/qwen3.5-abliterated\\:35b-a3b}") String modelName) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .build();
    }
}
