package cn.lysoy.jingu3.aspect;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.component.ChatRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 聊天入口：限流（降级为拒绝）+ 访问耗时日志，避免在 Controller 中重复样板代码。
 */
@Slf4j
@Aspect
@Component
@Order(0)
public class ChatInboundAspect {

    private final ChatRateLimiter chatRateLimiter;

    public ChatInboundAspect(ChatRateLimiter chatRateLimiter) {
        this.chatRateLimiter = chatRateLimiter;
    }

    @Around("@within(cn.lysoy.jingu3.common.annotation.ChatInboundApi)")
    public Object aroundChatInbound(ProceedingJoinPoint pjp) throws Throwable {
        String ip = resolveClientIp();
        if (!chatRateLimiter.tryAcquire(ip)) {
            throw new ServiceException(ErrorCode.TOO_MANY_REQUESTS, EngineMessages.CHAT_RATE_LIMITED);
        }
        long t0 = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            double ms = (System.nanoTime() - t0) / 1_000_000.0;
            log.info(
                    "chatInbound method={} clientIp={} durationMs={}",
                    pjp.getSignature().toShortString(),
                    ip,
                    String.format("%.2f", ms));
        }
    }

    private static String resolveClientIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return "n/a";
        }
        HttpServletRequest req = servletAttrs.getRequest();
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return req.getRemoteAddr();
    }
}
