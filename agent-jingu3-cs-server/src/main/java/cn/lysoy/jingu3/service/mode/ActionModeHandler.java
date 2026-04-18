package cn.lysoy.jingu3.service.mode;

import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.guard.ModeRegistry;

/**
 * 单一「行动模式」的<strong>执行契约</strong>，与《AI智能体行动模式设计指南》中八大模式一一对应（ASK、REACT、
 * PLAN_AND_EXECUTE、WORKFLOW、AGENT_TEAM、CRON、STATE_TRACKING、HUMAN_IN_LOOP）。
 * <p>
 * <strong>职责边界</strong>：实现类根据 {@link ExecutionContext} 组装提示词（经 {@link cn.lysoy.jingu3.service.prompt.PromptAssembly}）、
 * 调用 LangChain4j 模型与可选 {@link cn.lysoy.jingu3.skill.tool.ToolRegistry}，产出本轮助手可见文本或流式事件。
 * 同步 HTTP 路径走 {@link #execute}；SSE/WebSocket 走 {@link #stream}，由 {@link cn.lysoy.jingu3.service.context.chat.ChatStreamService}
 * 统一调度，属于<strong>驾驭工程</strong>中与传输形态解耦的「模式内核」。
 * </p>
 *
 * @see ActionMode
 * @see ModeRegistry
 */
public interface ActionModeHandler {

    /**
     * 在当前上下文下生成完整助手回复（阻塞式，一次返回整段字符串）。
     * <p>流式场景请使用 {@link #stream} 与 {@link cn.lysoy.jingu3.service.context.stream.StreamEventSink}。</p>
     *
     * @param context 单次调用的用户、会话、路由来源、模式及可选 workflowId 等
     * @return 助手回复正文，由上层封装为 {@link cn.lysoy.jingu3.common.vo.ChatVo#getReply()} 等
     */
    String execute(ExecutionContext context);

    /**
     * 流式执行本模式：向 {@link cn.lysoy.jingu3.service.context.stream.StreamEventSink} 推送 TOKEN、BLOCK、TOOL_RESULT、分步 step 等事件，
     * 最后须调用 {@link cn.lysoy.jingu3.service.context.stream.StreamEventSink#done()} 或 {@link cn.lysoy.jingu3.service.context.stream.StreamEventSink#error(String)}。
     *
     * @param context 与 {@link #execute} 相同语义
     * @param sink    与当前 HTTP SSE 或 WebSocket 会话绑定的接收端
     */
    void stream(ExecutionContext context, StreamEventSink sink);
}
