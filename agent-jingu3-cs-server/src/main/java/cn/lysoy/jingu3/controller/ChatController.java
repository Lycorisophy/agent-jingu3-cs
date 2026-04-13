package cn.lysoy.jingu3.controller;

import cn.lysoy.jingu3.common.annotation.ChatInboundApi;
import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.constant.ChatHttpHeaders;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.component.ChatInboundPlatformSupport;
import cn.lysoy.jingu3.common.vo.ChatVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.ChatService;
import cn.lysoy.jingu3.service.ChatStreamService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 对话 REST 入口：{@code POST /api/v1/chat} 返回完整 JSON（兼容旧客户端）；
 * {@code POST /api/v1/chat/stream} 返回 SSE，事件体与 WebSocket 一致，见 {@link cn.lysoy.jingu3.stream.StreamEvent}。
 */
@RestController
@RequestMapping("/api/v1")
@ChatInboundApi
public class ChatController {

    private final ChatService chatService;
    private final ChatStreamService chatStreamService;
    private final Jingu3Properties jingu3Properties;

    public ChatController(
            ChatService chatService, ChatStreamService chatStreamService, Jingu3Properties jingu3Properties) {
        this.chatService = chatService;
        this.chatStreamService = chatStreamService;
        this.jingu3Properties = jingu3Properties;
    }

    /**
     * 同步一轮对话，体与 {@link ChatRequest} 一致；内部走意图路由与 {@link cn.lysoy.jingu3.engine.ModeRegistry}。
     */
    @PostMapping("/chat")
    public ApiResult<ChatVo> chat(
            @RequestHeader(value = ChatHttpHeaders.CLIENT_PLATFORM, required = false) String clientPlatform,
            @Valid @RequestBody ChatRequest request) {
        ChatInboundPlatformSupport.mergeIntoRequest(request, clientPlatform);
        return ApiResult.ok(chatService.chat(request));
    }

    /**
     * SSE 流式：长连接默认 10 分钟，事件 payload 为 {@code application/json} 的 {@link cn.lysoy.jingu3.stream.StreamEvent}。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestHeader(value = ChatHttpHeaders.CLIENT_PLATFORM, required = false) String clientPlatform,
            @Valid @RequestBody ChatRequest request) {
        ChatInboundPlatformSupport.mergeIntoRequest(request, clientPlatform);
        SseEmitter emitter = new SseEmitter(jingu3Properties.getHttp().getSseEmitterTimeoutMs());
        chatStreamService.startSseStream(request, emitter);
        return emitter;
    }
}
