package cn.lysoy.jingu3.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * 解析 Ask 路由 JSON 与 ReAct 页脚 JSON；与 {@link PromptTemplates} 中的格式约定一致。
 */
public final class ToolRoutingParser {

    /** ReAct 步输出中 JSON 载荷的前导标记（单独一行），下一行或同段内为 JSON */
    public static final String JINGU3_JSON_MARKER = "<<<JINGU3_JSON>>>";

    private ToolRoutingParser() {
    }

    /** Ask：整段模型输出应仅含一行 JSON；失败时返回 empty，调用方宜回落直答 */
    public static Optional<AskRoutePayload> parseAskRoute(String raw, ObjectMapper om) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String json = extractBalancedJson(raw.trim());
        if (json == null) {
            return Optional.empty();
        }
        try {
            JsonNode n = om.readTree(json);
            String route = n.path("route").asText("").trim();
            if ("direct".equalsIgnoreCase(route)) {
                return Optional.of(AskRoutePayload.direct());
            }
            if ("tool".equalsIgnoreCase(route)) {
                String toolId = n.path("toolId").asText(null);
                String input = n.path("input").asText("");
                if (toolId == null || toolId.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(AskRoutePayload.tool(toolId, input));
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /** ReAct：优先解析标记后的 JSON，否则尝试从全文提取最后一个 JSON 对象 */
    public static Optional<ReactFooterPayload> parseReactFooter(String modelOutput, ObjectMapper om) {
        if (modelOutput == null || modelOutput.isBlank()) {
            return Optional.empty();
        }
        String json = extractJsonAfterMarker(modelOutput);
        if (json == null) {
            json = extractLastBalancedJson(modelOutput);
        }
        if (json == null) {
            return Optional.empty();
        }
        try {
            JsonNode n = om.readTree(json);
            String action = n.path("action").asText("").trim();
            if ("done".equalsIgnoreCase(action)) {
                return Optional.of(ReactFooterPayload.done());
            }
            if ("invoke".equalsIgnoreCase(action)) {
                String toolId = n.path("toolId").asText(null);
                String input = n.path("input").asText("");
                if (toolId == null || toolId.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(ReactFooterPayload.invoke(toolId, input));
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static String extractJsonAfterMarker(String text) {
        int m = text.indexOf(JINGU3_JSON_MARKER);
        if (m < 0) {
            return null;
        }
        String rest = text.substring(m + JINGU3_JSON_MARKER.length()).trim();
        return extractBalancedJson(rest);
    }

    /** 从最后一个 '{' 起向前平衡括号（处理模型在 JSON 前输出解释性文字的情况） */
    private static String extractLastBalancedJson(String text) {
        int end = text.lastIndexOf('}');
        if (end < 0) {
            return null;
        }
        int depth = 0;
        for (int i = end; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '}') {
                depth++;
            } else if (c == '{') {
                depth--;
                if (depth == 0) {
                    return text.substring(i, end + 1);
                }
            }
        }
        return null;
    }

    private static String extractBalancedJson(String text) {
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    /** Ask 路由载荷 */
    public record AskRoutePayload(boolean useTool, String toolId, String input) {

        public static AskRoutePayload direct() {
            return new AskRoutePayload(false, null, "");
        }

        public static AskRoutePayload tool(String toolId, String input) {
            return new AskRoutePayload(true, toolId, input == null ? "" : input);
        }
    }

    /** ReAct 页脚 */
    public record ReactFooterPayload(String action, String toolId, String input) {

        public static ReactFooterPayload done() {
            return new ReactFooterPayload("done", null, "");
        }

        public static ReactFooterPayload invoke(String toolId, String input) {
            return new ReactFooterPayload("invoke", toolId, input == null ? "" : input);
        }
    }
}
