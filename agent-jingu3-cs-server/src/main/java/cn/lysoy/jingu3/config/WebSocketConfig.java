package cn.lysoy.jingu3.config;

import cn.lysoy.jingu3.controller.app.ChatStreamWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册 {@code /ws/v1/chat} 原生 WebSocket：首帧 JSON 与 {@code POST /chat} 请求体相同，后续推送与 SSE 共用
 * {@link cn.lysoy.jingu3.service.context.stream.StreamEvent}。生产环境应收紧 {@code addHandler(...).setAllowedOriginPatterns(...)}。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatStreamWebSocketHandler chatStreamWebSocketHandler;

    public WebSocketConfig(ChatStreamWebSocketHandler chatStreamWebSocketHandler) {
        this.chatStreamWebSocketHandler = chatStreamWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatStreamWebSocketHandler, "/ws/v1/chat")
                .setAllowedOriginPatterns("*");
    }
}
