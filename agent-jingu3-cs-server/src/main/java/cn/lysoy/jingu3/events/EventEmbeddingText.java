package cn.lysoy.jingu3.events;

import cn.lysoy.jingu3.rag.entity.EventEntryEntity;

/**
 * 事件写入/检索共用的嵌入文本拼接（与 Milvus 向量一致）。
 */
public final class EventEmbeddingText {

    private EventEmbeddingText() {}

    /**
     * 用于 Ollama embed 的单段文本：优先 action/result/subject/location。
     */
    public static String forEmbedding(EventEntryEntity e) {
        StringBuilder sb = new StringBuilder();
        if (e.getAction() != null && !e.getAction().isBlank()) {
            sb.append(e.getAction().trim()).append('\n');
        }
        if (e.getResult() != null && !e.getResult().isBlank()) {
            sb.append(e.getResult().trim()).append('\n');
        }
        if (e.getEventSubject() != null && !e.getEventSubject().isBlank()) {
            sb.append(e.getEventSubject().trim()).append('\n');
        }
        if (e.getEventLocation() != null && !e.getEventLocation().isBlank()) {
            sb.append(e.getEventLocation().trim()).append('\n');
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? "event" : s;
    }

    /** 词法过滤用拼接（小写比较在调用方） */
    public static String searchBlob(EventEntryEntity e) {
        StringBuilder sb = new StringBuilder();
        append(sb, e.getAction());
        append(sb, e.getResult());
        append(sb, e.getEventSubject());
        append(sb, e.getEventLocation());
        append(sb, e.getAssertion());
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (part != null && !part.isBlank()) {
            sb.append(part).append(' ');
        }
    }
}
