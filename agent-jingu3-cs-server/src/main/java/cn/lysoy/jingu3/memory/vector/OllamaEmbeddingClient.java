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
 * 调用 Ollama {@code POST /api/embeddings} 生成向量。
 */
@Slf4j
@Component
public class OllamaEmbeddingClient {

    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    private final Jingu3Properties properties;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public OllamaEmbeddingClient(Jingu3Properties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("embed text empty");
        }
        String base = properties.getOllama().getBaseUrl().replaceAll("/$", "");
        String model = properties.getMemory().getEmbeddingModel();
        try {
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
