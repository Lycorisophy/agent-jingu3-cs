package cn.lysoy.jingu3.common.constant;

/**
 * 对话入口 HTTP/WebSocket 握手可用请求头（与客户端约定）。
 */
public final class ChatHttpHeaders {

    /**
     * 客户端平台标识（如 web、ios、android、desktop）；HTTP 用 Header，WebSocket 可握手携带，浏览器亦可放在 {@link cn.lysoy.jingu3.common.dto.ChatRequest#getClientPlatform()}。
     */
    public static final String CLIENT_PLATFORM = "X-Jingu3-Client-Platform";

    private ChatHttpHeaders() {
    }
}
