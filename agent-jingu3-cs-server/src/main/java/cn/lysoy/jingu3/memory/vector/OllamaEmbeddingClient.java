package cn.lysoy.jingu3.memory.vector;

import cn.lysoy.jingu3.config.Jingu3Properties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 记忆向量链路的<strong>嵌入端适配器</strong>：调用本地 Ollama {@code POST /api/embeddings}，将文本转为 float 向量，
 * 供 {@link MilvusMemoryVectorService} 等写入/检索 Milvus。属于上下文工程中「可检索长期记忆」的数值化入口，与对话模型分离配置。
 */
@Slf4j
@Component
public class OllamaEmbeddingClient {

    /** 单次嵌入请求上限，避免大段文本阻塞过久（与 HTTP client 行为一致）。 */
    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    private final Jingu3Properties properties;

    private final ObjectMapper objectMapper;

    /** 复用连接；连接超时与单次请求 {@link #TIMEOUT} 分离配置。 */
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public OllamaEmbeddingClient(Jingu3Properties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 对整段文本生成一条向量；调用方负责分块策略（若有）。模型名取自 {@code jingu3.memory.embedding-model}。
     *
     * @param text 非空非空白待嵌入文本
     * @return 与当前嵌入模型维度一致的 float 数组
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("embed text empty");
        }
        String base = properties.getOllama().getBaseUrl().replaceAll("/$", "");
        String model = properties.getMemory().getEmbeddingModel();
        try {
            // Ollama embeddings API：model + prompt(JSON)，返回根节点下 embedding 数组
            String body = objectMapper.createObjectNode().put("model", model).put("prompt", text).toString();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/api/embeddings"))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() / 100 != 2) {
                throw new IllegalStateException("Ollama embeddings HTTP " + resp.statusCode() + ": " + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode emb = root.get("embedding");
            if (emb == null || !emb.isArray()) {
                throw new IllegalStateException("Ollama embeddings: missing embedding array");
            }
            List<Float> list = new ArrayList<>();
            for (JsonNode n : emb) {
                list.add((float) n.asDouble());
            }
            float[] out = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                out[i] = list.get(i);
            }
            return out;
        } catch (Exception e) {
            log.error("Ollama embed failed model={}", model, e);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
