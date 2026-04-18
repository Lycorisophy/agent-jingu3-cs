package cn.lysoy.jingu3.service.guard;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.service.guard.routing.RoutingSource;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 单次「对话步」的<strong>执行上下文</strong>（上下文工程在引擎侧的载体）：把意图路由结果、用户侧文本、
 * 可选编排链负载与工作流 id 一并交给 {@link ActionModeHandler}，供八大行动模式中任一模式消费。
 * <p>
 * <strong>与上层的关系</strong>：{@link cn.lysoy.jingu3.service.context.chat.ChatService} / {@link cn.lysoy.jingu3.service.context.chat.ChatStreamService}
 * 在校验与路由之后构造本对象，再经 {@link cn.lysoy.jingu3.service.guard.ModeRegistry} 分派到具体模式处理器，属于
 * 「驾驭工程」中连接 HTTP/WebSocket 与模式实现的枢纽类型。
 * </p>
 * <p>
 * 指南 §1.4 中的分层记忆（STM/Episodic/LTM）尚未完全落地时，{@link #history} 仍为占位列表；记忆注入主要发生在
 * {@link cn.lysoy.jingu3.service.context.prepare.UserPromptPreparationService}（送模前的用户串改写），与本字段解耦。
 * </p>
 */
@Getter
public class ExecutionContext {

    /** 单用户阶段与配置种子一致的用户 id（见 {@link cn.lysoy.jingu3.component.UserConstants}） */
    private final String userId;
    /** 展示名；送模提示中可与 userId 一并出现 */
    private final String username;
    /** 会话标识；State Tracking、日志、密文落库等横切能力使用 */
    private final String conversationId;
    /**
     * 用户本轮原始消息（未经记忆向量检索扩写时即为请求体；经 {@link cn.lysoy.jingu3.service.context.prepare.UserPromptPreparationService}
     * 处理后传入构造器的常为「已增强」串）。
     */
    private final String userMessage;
    /** 本步选用的行动模式（由 {@link cn.lysoy.jingu3.service.guard.routing.IntentRouter}、显式 mode、modePlan 或回落逻辑决定） */
    private final ActionMode selectedMode;
    /** 该模式来自客户端显式、关键词规则、模型分类、降级或显式守门等，供 {@link cn.lysoy.jingu3.service.prompt.ModeRoutingPreamble} 生成对模型的说明 */
    private final RoutingSource routingSource;
    /**
     * 多轮对话历史摘要或原文片段（v0.2 占位；完整 STM/会话摘要策略见路线图记忆史诗）。
     */
    private final List<String> history;
    /**
     * <strong>编排链 / modePlan</strong> 上一步的拼接负载：由 {@link cn.lysoy.jingu3.service.mode.orchestration.ModePlanExecutor}
     * 与流式 {@link cn.lysoy.jingu3.service.context.chat.ChatStreamService#streamModePlan} 写入，格式为「用户原问 + 分隔符 + 上步模型答复」。
     * 非空时 {@link #llmInput()} 优先返回本字段，使下一步模型具备链式上下文（指南 §11）。
     */
    private final String taskPayload;
    /** 选用 {@link ActionMode#WORKFLOW} 时必填的工作流定义 id；缺失时路由层可回落为 ASK，见 {@link cn.lysoy.jingu3.service.guard.routing.RoutingFallbacks} */
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
     * 拼出本步应送入「主推理模型」的用户侧输入：若存在 modePlan 链式负载则优先用其，否则用 {@link #userMessage}。
     */
    public String llmInput() {
        // 编排场景：taskPayload 已由上层拼接，避免下一步仍只看到原始 userMessage
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
