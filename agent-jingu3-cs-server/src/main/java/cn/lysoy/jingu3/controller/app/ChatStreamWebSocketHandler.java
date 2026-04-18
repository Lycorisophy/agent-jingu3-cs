package cn.lysoy.jingu3.controller.app;

import cn.lysoy.jingu3.common.constant.ChatHttpHeaders;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.component.ChatInboundPlatformSupport;
import cn.lysoy.jingu3.service.context.chat.ChatStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 文本处理器：连接建立后等待客户端发送<strong>一条</strong> JSON（结构同 {@link ChatRequest}），
 * 校验通过后异步执行 {@link cn.lysoy.jingu3.service.context.chat.ChatStreamService#startWebSocketStream}。
 * 解析失败时以 {@link org.springframework.web.socket.CloseStatus#BAD_DATA} 关闭。
 */
@Slf4j
@Component
public class ChatStreamWebSocketHandler extends TextWebSocketHandler {

    private final ChatStreamService chatStreamService;
    private final ObjectMapper objectMapper;

    public ChatStreamWebSocketHandler(ChatStreamService chatStreamService, ObjectMapper objectMapper) {
        this.chatStreamService = chatStreamService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("ws connected id={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ChatRequest request = objectMapper.readValue(message.getPayload(), ChatRequest.class);
            String platformHeader = session.getHandshakeHeaders().getFirst(ChatHttpHeaders.CLIENT_PLATFORM);
            ChatInboundPlatformSupport.mergeIntoRequest(request, platformHeader);
            chatStreamService.startWebSocketStream(request, session);
        } catch (Exception e) {
            log.warn("ws parse or stream failed: {}", e.toString());
            try {
                session.close(CloseStatus.BAD_DATA.withReason(e.getMessage()));
            } catch (Exception ignored) {
                // ignore
            }
        }
    }
}
