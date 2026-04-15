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
 * <strong>指南 §9 State Tracking</strong>（八大行动模式之一；<strong>占位实现</strong>）：目标为跨轮次会话状态与轨迹，
 * 与 DST（{@code /api/v1/dst}）等持久化能力协同；当前 Handler 仅维护 {@link ExecutionContext#getConversationId()} →
 * 交互次数的<strong>进程内</strong>计数，<strong>进程重启即丢失</strong>，用于演示与联调占位。
 */
@Component
public class StateTrackingModeHandler implements ActionModeHandler {

    /** 会话 id → 累计交互次数（非集群安全；多实例部署时需改为 Redis/DB） */
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
