package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.engine.routing.RoutingSource;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 单次「对话步」的执行上下文：把路由结果与用户输入打包传给 {@link ActionModeHandler}。
 * <p>
 * 指南 §1.4 中的分层记忆（STM/Episodic/LTM）尚未落地时，{@link #history} 仅为占位列表，供后续史诗接入。
 * </p>
 */
@Getter
public class ExecutionContext {

    private final String userId;
    private final String username;
    /** 会话标识；State Tracking、日志等使用 */
    private final String conversationId;
    /** 用户本轮原始消息 */
    private final String userMessage;
    /** 本步选用的行动模式（已由 {@link cn.lysoy.jingu3.engine.routing.IntentRouter} 等决定） */
    private final ActionMode selectedMode;
    /** 该模式来自客户端显式、规则还是模型分类等 */
    private final RoutingSource routingSource;
    /**
     * 多轮对话历史摘要或原文片段（v0.2 占位；注入策略见路线图）。
     */
    private final List<String> history;
    /**
     * 编排链上一步的拼接负载（仅 {@link cn.lysoy.jingu3.engine.orchestration.ModePlanExecutor} 等多步编排使用）。
     * 非空时 {@link #llmInput()} 优先返回本字段，以便下一步模型看到「用户问题 + 上一步答复」。
     */
    private final String taskPayload;
    /** 选用 {@link ActionMode#WORKFLOW} 时由请求传入的工作流定义 id */
    private final String workflowId;

    public ExecutionContext(
            String userId,
            String username,
            String conversationId,
            String userMessage,
            ActionMode selectedMode,
            RoutingSource routingSource,
            List<String> history,
            String taskPayload,
            String workflowId) {
        this.userId = userId;
        this.username = username;
        this.conversationId = conversationId;
        this.userMessage = userMessage;
        this.selectedMode = selectedMode;
        this.routingSource = routingSource;
        this.history = history == null ? List.of() : List.copyOf(history);
        this.taskPayload = taskPayload;
        this.workflowId = workflowId;
    }

    /**
     * 拼 LLM 输入：编排链存在时用上一步链接结果，否则用用户原话。
     */
    public String llmInput() {
        return (taskPayload == null || taskPayload.isBlank()) ? userMessage : taskPayload;
    }

    /**
     * 构造仅用于测试或最小场景的上下文（无 history / taskPayload / workflowId）。
     */
    public static ExecutionContext minimal(
            String userId,
            String username,
            String userMessage,
            ActionMode mode,
            RoutingSource source) {
        return new ExecutionContext(
                userId, username, ConversationConstants.DEFAULT_CONVERSATION_ID, userMessage, mode, source,
                Collections.emptyList(), null, null);
    }
}
