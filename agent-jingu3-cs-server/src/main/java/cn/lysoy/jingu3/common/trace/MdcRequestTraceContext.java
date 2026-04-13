package cn.lysoy.jingu3.common.trace;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import org.slf4j.MDC;

/**
 * 将请求 ID / 追踪 ID 写入 {@link MDC}，供 Log4j2 模式 {@code %X{requestId}} / {@code %X{traceId}} 使用。
 */
public final class MdcRequestTraceContext {

    private MdcRequestTraceContext() {
    }

    /**
     * 从 JSON 体覆盖（若字段非空）：应在 Filter 初始化 MDC 之后、业务前调用。
     */
    public static void mergeFromBody(ChatRequest request) {
        if (request == null) {
            return;
        }
        String rid = trimToNull(request.getRequestId());
        String tid = trimToNull(request.getTraceId());
        if (rid != null) {
            MDC.put(MdcKeys.REQUEST_ID, rid);
            request.setRequestId(rid);
        }
        if (tid != null) {
            MDC.put(MdcKeys.TRACE_ID, tid);
            request.setTraceId(tid);
        }
    }

    /**
     * WebSocket 文本消息无 Servlet Filter：若体中缺省则生成雪花 ID；追踪 ID 缺省时与请求 ID 相同。
     */
    public static void ensureWebSocketMessage(ChatRequest request, SnowflakeIdGenerator snowflake) {
        if (request == null) {
            return;
        }
        String rid = trimToNull(request.getRequestId());
        if (rid == null) {
            rid = snowflake.nextIdString();
            request.setRequestId(rid);
        }
        String tid = trimToNull(request.getTraceId());
        if (tid == null) {
            tid = rid;
            request.setTraceId(tid);
        }
        MDC.put(MdcKeys.REQUEST_ID, rid);
        MDC.put(MdcKeys.TRACE_ID, tid);
    }

    public static void clear() {
        MDC.remove(MdcKeys.REQUEST_ID);
        MDC.remove(MdcKeys.TRACE_ID);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
