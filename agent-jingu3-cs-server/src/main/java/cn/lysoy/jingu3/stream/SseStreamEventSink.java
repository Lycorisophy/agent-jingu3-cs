package cn.lysoy.jingu3.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * {@link StreamEventSink} 的 SSE 适配：每条事件序列化为 JSON 字符串后作为
 * {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter#event()} 的 data 下发。
 * <p>客户端可用 EventSource 或 fetch 流解析；{@link #done()} 与 {@link #error(String)} 会结束 emitter。</p>
 */
public class SseStreamEventSink implements StreamEventSink {

    private final SseEmitter emitter;
    private final ObjectMapper objectMapper;

    public SseStreamEventSink(SseEmitter emitter, ObjectMapper objectMapper) {
        this.emitter = emitter;
        this.objectMapper = objectMapper;
    }

    /** 单条发送；写失败则认为客户端已断开，completeWithError 收尾 */
    private void emit(StreamEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .data(objectMapper.writeValueAsString(event), MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    @Override
    public void meta(String actionMode, String routingSource, String userId, String username) {
        emit(StreamEvent.meta(actionMode, routingSource, userId, username));
    }

    @Override
    public void token(String delta) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        emit(StreamEvent.token(delta));
    }

    @Override
    public void stepBegin(int step, String label) {
        emit(StreamEvent.stepBegin(step, label));
    }

    @Override
    public void stepEnd(int step) {
        emit(StreamEvent.stepEnd(step));
    }

    @Override
    public void block(String text) {
        emit(StreamEvent.block(text == null ? "" : text));
    }

    @Override
    public void toolResult(String toolId, String toolOutput) {
        emit(StreamEvent.toolResult(
                toolId == null ? "" : toolId, toolOutput == null ? "" : toolOutput));
    }

    @Override
    public void done() {
        emit(StreamEvent.done());
        emitter.complete();
    }

    @Override
    public void error(String message) {
        emit(StreamEvent.error(message == null ? "unknown error" : message));
        emitter.complete();
    }
}
