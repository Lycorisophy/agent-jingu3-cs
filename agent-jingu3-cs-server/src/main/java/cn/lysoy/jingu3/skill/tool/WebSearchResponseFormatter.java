package cn.lysoy.jingu3.skill.tool;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 DuckDuckGo / Tavily 的 JSON 转为给模型阅读的短文本（同包单测可覆盖）。
 */
final class WebSearchResponseFormatter {

    private static final int MAX_OUTPUT_CHARS = 8000;
    private static final int MAX_SNIPPET_CHARS = 500;

    private WebSearchResponseFormatter() {
    }

    static String formatDuckDuckGo(JsonNode root, int maxResults) {
        if (maxResults < 1) {
            maxResults = 1;
        }
        String heading = textOrEmpty(root, "Heading");
        String absText = textOrEmpty(root, "AbstractText");
        String absUrl = textOrEmpty(root, "AbstractURL");

        StringBuilder head = new StringBuilder();
        if (!heading.isEmpty()) {
            head.append(heading).append("\n\n");
        }
        if (!absText.isEmpty()) {
            head.append(absText);
            if (!absUrl.isEmpty()) {
                head.append("\n").append(absUrl);
            }
        }

        List<String> lines = new ArrayList<>();
        if (root.has("Results") && root.get("Results").isArray()) {
            collectDdgTopic(root.get("Results"), lines, maxResults);
        }
        int remaining = maxResults - lines.size();
        if (remaining > 0 && root.has("RelatedTopics") && root.get("RelatedTopics").isArray()) {
            collectDdgTopic(root.get("RelatedTopics"), lines, remaining);
        }

        if (head.length() == 0 && lines.isEmpty()) {
            return "未检索到可用的即时摘要或相关条目，请尝试更换关键词。";
        }

        StringBuilder out = new StringBuilder();
        if (head.length() > 0) {
            out.append(head);
            if (!lines.isEmpty()) {
                out.append("\n\n---\n\n");
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out.append("\n\n");
            }
            out.append(i + 1).append(". ").append(lines.get(i));
        }
        return truncate(out.toString(), MAX_OUTPUT_CHARS);
    }

    static String formatTavily(JsonNode root, int maxResults) {
        if (maxResults < 1) {
            maxResults = 1;
        }
        JsonNode results = root.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            return "未检索到结果，请尝试更换关键词。";
        }
        int n = Math.min(maxResults, results.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            JsonNode r = results.get(i);
            String title = textOrEmpty(r, "title");
            String url = textOrEmpty(r, "url");
            String content = textOrEmpty(r, "content");
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append(i + 1).append(". ");
            if (!title.isEmpty()) {
                sb.append(title).append("\n");
            }
            if (!url.isEmpty()) {
                sb.append(url).append("\n");
            }
            if (!content.isEmpty()) {
                sb.append(truncate(content, MAX_SNIPPET_CHARS));
            }
        }
        return truncate(sb.toString(), MAX_OUTPUT_CHARS);
    }

    private static void collectDdgTopic(JsonNode n, List<String> lines, int max) {
        if (lines.size() >= max || n == null || n.isNull()) {
            return;
        }
        if (n.isArray()) {
            for (JsonNode c : n) {
                collectDdgTopic(c, lines, max);
                if (lines.size() >= max) {
                    return;
                }
            }
            return;
        }
        if (!n.isObject()) {
            return;
        }
        if (n.has("Topics") && n.get("Topics").isArray()) {
            collectDdgTopic(n.get("Topics"), lines, max);
            return;
        }
        String text = textOrEmpty(n, "Text");
        if (text.isEmpty()) {
            return;
        }
        String url = textOrEmpty(n, "FirstURL");
        String line = url.isEmpty() ? text : text + "\n" + url;
        lines.add(line);
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || !v.isTextual()) {
            return "";
        }
        return v.asText().trim();
    }

    private static String truncate(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "…";
    }
}
