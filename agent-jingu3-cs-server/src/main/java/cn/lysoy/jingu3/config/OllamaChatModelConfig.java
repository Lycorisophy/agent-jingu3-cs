package cn.lysoy.jingu3.config;

import cn.lysoy.jingu3.common.constant.Jingu3ConfigKeys;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地 Ollama HTTP API 对接：注册阻塞式 {@link ChatLanguageModel} 与流式 {@link StreamingChatLanguageModel} 两枚 Bean，
 * 供各模式 handler 与 Ask 流式路径注入。二者应共用同一 {@code baseUrl} 与 {@code modelName}，避免行为不一致。
 * <p>配置键前缀见 {@link Jingu3ConfigKeys}。</p>
 */
@Configuration
public class OllamaChatModelConfig {

    /**
     * 非流式：意图分类、ReAct/Plan 等多步阻塞调用均使用此 Bean。
     */
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

    /**
     * 流式：与 {@link dev.langchain4j.model.ollama.OllamaStreamingChatModel} 一致，底层即 Ollama {@code stream:true}。
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(
            @Value("${jingu3.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${jingu3.ollama.chat-model:gnremy/qwen3.5-abliterated\\:35b-a3b}") String modelName) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .build();
    }
}
