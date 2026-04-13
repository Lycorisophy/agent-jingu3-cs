package cn.lysoy.jingu3.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * {@link StreamEventSink} 的 WebSocket 适配：与 {@link SseStreamEventSink} 序列化同一 JSON，
 * 便于前端共用解析逻辑；{@link #done()} / {@link #error(String)} 会关闭会话（单轮一问一答模型）。
 */
public class WebSocketStreamEventSink implements StreamEventSink {

    private final WebSocketSession session;
    private final ObjectMapper objectMapper;

    public WebSocketStreamEventSink(WebSocketSession session, ObjectMapper objectMapper) {
        this.session = session;
        this.objectMapper = objectMapper;
    }

    private void emit(StreamEvent event) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException ignored) {
                // ignore
            }
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
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
            // ignore
        }
    }

    @Override
    public void error(String message) {
        emit(StreamEvent.error(message == null ? "unknown error" : message));
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
            // ignore
        }
    }
}
