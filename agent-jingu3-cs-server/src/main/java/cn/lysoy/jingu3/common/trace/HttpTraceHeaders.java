package cn.lysoy.jingu3.common.trace;

/**
 * 请求 / 响应中与链路追踪相关的 HTTP 头（小写 HTTP 头名在 Servlet 中不区分大小写）。
 */
public final class HttpTraceHeaders {

    /** 客户端传入或网关下发的请求 ID；缺失时服务端生成雪花 ID */
    public static final String REQUEST_ID = "X-Request-Id";

    /** 分布式追踪 ID；缺失时与请求 ID 相同或由服务端生成 */
    public static final String TRACE_ID = "X-Trace-Id";

    private HttpTraceHeaders() {
    }
}
