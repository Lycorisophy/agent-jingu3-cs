package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.stream.StreamEventSink;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指南 §9 State Tracking：跨轮次状态与轨迹；本实现为按 {@link ExecutionContext#getConversationId()}
 * 维度的进程内计数器，重启即丢失。异步管道与持久化轨迹依赖记忆/存储史诗。
 */
@Component
public class StateTrackingModeHandler implements ActionModeHandler {

    private final ConcurrentHashMap<String, AtomicLong> interactionCount = new ConcurrentHashMap<>();

    /**
     * 每调用一次对当前会话计数 +1，并返回格式化说明（不调用 LLM）。
     */
    @Override
    public String execute(ExecutionContext context) {
        String conv = context.getConversationId() == null || context.getConversationId().isBlank()
                ? ConversationConstants.DEFAULT_CONVERSATION_ID
                : context.getConversationId();
        long n = interactionCount.computeIfAbsent(conv, k -> new AtomicLong()).incrementAndGet();
        return String.format(EngineMessages.STATE_TRACKING_REPLY, conv, n);
    }

    @Override
    public void stream(ExecutionContext context, StreamEventSink sink) {
        sink.stepBegin(1, "state_tracking");
        sink.block(execute(context));
        sink.stepEnd(1);
        sink.done();
    }
}
