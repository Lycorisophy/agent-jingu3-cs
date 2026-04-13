package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.trace.HttpTraceHeaders;
import cn.lysoy.jingu3.common.trace.MdcKeys;
import cn.lysoy.jingu3.common.trace.SnowflakeIdGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 入口：解析或生成请求 ID、追踪 ID，写入 MDC 与响应头；优先级最高，早于其它 Servlet 过滤器。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceRequestIdFilter extends OncePerRequestFilter {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public TraceRequestIdFilter(SnowflakeIdGenerator snowflakeIdGenerator) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestId = trimOrNull(request.getHeader(HttpTraceHeaders.REQUEST_ID));
            String traceId = trimOrNull(request.getHeader(HttpTraceHeaders.TRACE_ID));
            if (requestId == null) {
                requestId = snowflakeIdGenerator.nextIdString();
            }
            if (traceId == null) {
                traceId = requestId;
            }
            MDC.put(MdcKeys.REQUEST_ID, requestId);
            MDC.put(MdcKeys.TRACE_ID, traceId);
            response.setHeader(HttpTraceHeaders.REQUEST_ID, requestId);
            response.setHeader(HttpTraceHeaders.TRACE_ID, traceId);
            filterChain.doFilter(request, response);
        } finally {
            if (!request.isAsyncStarted()) {
                MDC.remove(MdcKeys.REQUEST_ID);
                MDC.remove(MdcKeys.TRACE_ID);
            }
        }
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
