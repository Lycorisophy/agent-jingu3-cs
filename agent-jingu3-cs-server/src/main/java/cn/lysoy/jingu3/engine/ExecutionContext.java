package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.engine.routing.RoutingSource;

import java.util.Collections;
import java.util.List;

/**
 * 单次对话执行上下文（v0.1 最小上下文，不实现记忆史诗）。
 */
public record ExecutionContext(
        String userId,
        String username,
        String conversationId,
        String userMessage,
        ActionMode selectedMode,
        RoutingSource routingSource,
        List<String> history
) {
    public ExecutionContext {
        history = history == null ? List.of() : List.copyOf(history);
    }

    public static ExecutionContext minimal(
            String userId,
            String username,
            String userMessage,
            ActionMode mode,
            RoutingSource source) {
        return new ExecutionContext(userId, username, "default", userMessage, mode, source, Collections.emptyList());
    }
}
