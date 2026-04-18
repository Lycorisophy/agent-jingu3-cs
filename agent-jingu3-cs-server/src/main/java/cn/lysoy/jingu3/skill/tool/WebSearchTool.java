package cn.lysoy.jingu3.skill.tool;

import cn.lysoy.jingu3.common.enums.ToolRiskLevel;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.config.WebSearchProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 联网搜索：默认 DuckDuckGo Instant Answer（免 Key）；可选 Tavily（需 API Key）。
 */
@Slf4j
public class WebSearchTool implements Jingu3Tool {

    private static final String UA = "Jingu3Agent/1.0 (web_search; +https://github.com/)";
    private static final int MAX_QUERY_CHARS = 500;

    private final Jingu3Properties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public WebSearchTool(Jingu3Properties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public String id() {
        return "web_search";
    }

    @Override
    public String description() {
        return "联网检索公开信息摘要。input 为搜索关键词或短问句；返回若干条标题、摘要与链接（用于事实核对）。"
                + "后端由配置选择 DuckDuckGo 或 Tavily。";
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.MEDIUM;
    }

    @Override
    public String execute(String input) throws ToolExecutionException {
        if (input == null || input.isBlank()) {
            throw new ToolExecutionException("web_search 需要非空搜索词");
        }
        String q = input.trim();
        if (q.length() > MAX_QUERY_CHARS) {
            throw new ToolExecutionException("搜索词过长（上限 " + MAX_QUERY_CHARS + " 字符）");
        }

        Jingu3Properties.Tool.WebSearch cfg = properties.getTool().getWebSearch();
        WebSearchProvider provider = cfg.getProvider() != null ? cfg.getProvider() : WebSearchProvider.DUCKDUCKGO;
        int timeoutSec = Math.max(1, cfg.getTimeoutSeconds());
        int maxResults = Math.max(1, cfg.getMaxResults());
        Duration timeout = Duration.ofSeconds(timeoutSec);

        try {
            if (provider == WebSearchProvider.TAVILY) {
                return searchTavily(q, cfg.getTavilyApiKey(), maxResults, timeout);
            }
            return searchDuckDuckGo(q, maxResults, timeout);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.warn("web_search failed: {}", e.toString());
            throw new ToolExecutionException("网络搜索失败: " + e.getMessage(), e);
        }
    }

    private String searchDuckDuckGo(String query, int maxResults, Duration timeout) throws Exception {
        String enc = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.duckduckgo.com/?q=" + enc + "&format=json&no_html=1&skip_disambig=1";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new ToolExecutionException("DuckDuckGo HTTP " + resp.statusCode());
        }
        JsonNode root = objectMapper.readTree(resp.body());
        return WebSearchResponseFormatter.formatDuckDuckGo(root, maxResults);
    }

    private String searchTavily(String query, String apiKey, int maxResults, Duration timeout) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ToolExecutionException("Tavily 需配置 jingu3.tool.web-search.tavily-api-key");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("api_key", apiKey.trim());
        body.put("query", query);
        body.put("max_results", maxResults);
        body.put("search_depth", "basic");
        String json = body.toString();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tavily.com/search"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("User-Agent", UA)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            throw new ToolExecutionException("Tavily HTTP " + resp.statusCode() + ": " + truncateForLog(resp.body()));
        }
        JsonNode root = objectMapper.readTree(resp.body());
        if (root.has("error")) {
            String err = root.get("error").asText("");
            throw new ToolExecutionException("Tavily 错误: " + (err.isEmpty() ? resp.body() : err));
        }
        return WebSearchResponseFormatter.formatTavily(root, maxResults);
    }

    private static String truncateForLog(String body) {
        if (body == null) {
            return "";
        }
        if (body.length() <= 200) {
            return body;
        }
        return body.substring(0, 200) + "…";
    }
}
