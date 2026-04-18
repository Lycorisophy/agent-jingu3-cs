package cn.lysoy.jingu3.service.context.chat;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内 STM：按会话 id 保留最近若干轮「用户原文 / 助手可见答复」，供 {@link cn.lysoy.jingu3.service.guard.ExecutionContext#getHistory()} 注入。
 * <p>多实例部署时不共享；生产可换 Redis 或 messages 表（见路线图 v0.6）。</p>
 */
@Component
public class ConversationStmBuffer {

    private static final int MAX_TURN_CHARS = 12000;

    private final Jingu3Properties properties;

    private final ConcurrentHashMap<String, Deque<Turn>> store = new ConcurrentHashMap<>();

    public ConversationStmBuffer(Jingu3Properties properties) {
        this.properties = properties;
    }

    /**
     * 丢弃该会话最近一轮用户+助手对（用于撤销上一轮再生成）。
     */
    public void dropLastTurn(String conversationId) {
        if (!properties.getChat().isStmEnabled()) {
            return;
        }
        String key = normalizeKey(conversationId);
        Deque<Turn> q = store.get(key);
        if (q == null || q.isEmpty()) {
            return;
        }
        q.pollLast();
    }

    /**
     * 供送模拼入的近期对话行（不含当前轮用户句）；每轮两行：用户：… / 助手：…
     */
    public List<String> snapshotLines(String conversationId) {
        if (!properties.getChat().isStmEnabled()) {
            return List.of();
        }
        String key = normalizeKey(conversationId);
        Deque<Turn> q = store.get(key);
        if (q == null || q.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Turn t : q) {
            out.add("用户：" + truncate(t.userRaw()));
            out.add("助手：" + truncate(t.assistant()));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * 在助手答复已知后写入一轮（应在对话主链路成功返回后调用）。
     */
    public void recordTurn(String conversationId, String userRaw, String assistantReply) {
        if (!properties.getChat().isStmEnabled()) {
            return;
        }
        String key = normalizeKey(conversationId);
        int maxPairs = Math.max(1, properties.getChat().getStmMaxPairs());
        Deque<Turn> q = store.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(new Turn(truncate(userRaw == null ? "" : userRaw), truncate(assistantReply == null ? "" : assistantReply)));
            while (q.size() > maxPairs) {
                q.pollFirst();
            }
        }
    }

    private static String normalizeKey(String conversationId) {
        return conversationId == null || conversationId.isBlank() ? "_" : conversationId.trim();
    }

    private static String truncate(String s) {
        if (s.length() <= MAX_TURN_CHARS) {
            return s;
        }
        return s.substring(0, MAX_TURN_CHARS) + "\n…(已截断)";
    }

    private record Turn(String userRaw, String assistant) {}
}
