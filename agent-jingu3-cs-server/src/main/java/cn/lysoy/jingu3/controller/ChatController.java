package cn.lysoy.jingu3.controller;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.ModeRegistry;
import cn.lysoy.jingu3.engine.routing.IntentRouter;
import cn.lysoy.jingu3.engine.routing.RoutingDecision;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.vo.ChatVo;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.1 对话 HTTP API。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final IntentRouter intentRouter;
    private final ModeRegistry modeRegistry;
    private final UserConstants userConstants;

    public ChatController(IntentRouter intentRouter, ModeRegistry modeRegistry, UserConstants userConstants) {
        this.intentRouter = intentRouter;
        this.modeRegistry = modeRegistry;
        this.userConstants = userConstants;
    }

    @PostMapping("/chat")
    public ApiResult<ChatVo> chat(@Valid @RequestBody ChatRequest request) {
        RoutingDecision decision = intentRouter.resolve(request.message(), request.mode());
        log.info("routing userId={} source={} mode={} conv={}",
                userConstants.getId(),
                decision.source(),
                decision.mode(),
                request.conversationId());

        var ctx = new ExecutionContext(
                userConstants.getId(),
                userConstants.getUsername(),
                request.conversationId() == null ? "default" : request.conversationId(),
                request.message(),
                decision.mode(),
                decision.source(),
                java.util.List.of()
        );
        var handler = modeRegistry.get(decision.mode());
        String reply = handler.execute(ctx);
        ChatVo vo = new ChatVo(
                userConstants.getId(),
                userConstants.getUsername(),
                reply,
                decision.mode().name(),
                decision.source().name()
        );
        return ApiResult.ok(vo);
    }
}
