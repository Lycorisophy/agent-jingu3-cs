package cn.lysoy.jingu3.aspect;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.trace.MdcRequestTraceContext;
import cn.lysoy.jingu3.common.trace.SnowflakeIdGenerator;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 将 {@link ChatRequest} 体中的 {@code requestId} / {@code traceId} 合并进 MDC（覆盖 Filter 默认值），
 * 满足「请求参数中带链路字段」的约定；WebSocket 无 Filter 时在体缺省时生成雪花 ID。
 */
@Aspect
@Component
@Order(5)
public class MdcChatRequestBodyAspect {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public MdcChatRequestBodyAspect(SnowflakeIdGenerator snowflakeIdGenerator) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Before("execution(* cn.lysoy.jingu3.service.ChatService.chat(..)) && args(request)")
    public void mergeChatBody(ChatRequest request) {
        MdcRequestTraceContext.mergeFromBody(request);
    }

    @Before("execution(* cn.lysoy.jingu3.service.ChatStreamService.startSseStream(..)) && args(request,..)")
    public void mergeSseBody(ChatRequest request) {
        MdcRequestTraceContext.mergeFromBody(request);
    }

    @Before("execution(* cn.lysoy.jingu3.service.ChatStreamService.startWebSocketStream(..)) && args(request,..)")
    public void mergeWsBody(ChatRequest request) {
        MdcRequestTraceContext.ensureWebSocketMessage(request, snowflakeIdGenerator);
    }
}
