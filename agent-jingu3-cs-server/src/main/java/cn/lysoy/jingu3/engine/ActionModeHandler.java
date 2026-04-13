package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.stream.StreamEventSink;

/**
 * 单一「行动模式」的执行契约，对应《AI智能体行动模式设计指南》中的八种模式之一。
 * <p>
 * 实现类负责：根据 {@link ExecutionContext} 调用 LLM（及后续工具/记忆等扩展），返回本轮助手可见文本。
 * 同步路径仅 {@link #execute}；需要打字机/分步观测时由各 handler 另行提供 {@code stream(...)}（见
 * {@link cn.lysoy.jingu3.service.ChatStreamService}）。
 * </p>
 *
 * @see ActionMode
 * @see ModeRegistry
 */
public interface ActionModeHandler {

    /**
     * 在当前上下文下生成完整助手回复（阻塞式，一次返回整段字符串）。
     * <p>流式场景请使用各模式 handler 的 {@code stream} 与 {@link cn.lysoy.jingu3.stream.StreamEventSink}。</p>
     *
     * @param context 单次调用的用户、会话、路由来源、模式及可选 workflowId 等
     * @return 助手回复正文，由上层封装为 {@link cn.lysoy.jingu3.common.vo.ChatVo#getReply()} 等
     */
    String execute(ExecutionContext context);

    void stream(ExecutionContext context, StreamEventSink sink);
}
