package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.dto.ChatRequest;

/**
 * 解析并写回 {@link ChatRequest#setClientPlatform(String)}：HTTP Header 优先于 JSON 体字段。
 */
public final class ChatInboundPlatformSupport {

    private ChatInboundPlatformSupport() {
    }

    /**
     * @param headerOrNull HTTP 头或 WebSocket 握手头中的平台标识
     */
    public static void mergeIntoRequest(ChatRequest request, String headerOrNull) {
        if (headerOrNull != null && !headerOrNull.isBlank()) {
            request.setClientPlatform(headerOrNull.trim());
            return;
        }
        if (request.getClientPlatform() == null || request.getClientPlatform().isBlank()) {
            request.setClientPlatform("unknown");
        } else {
            request.setClientPlatform(request.getClientPlatform().trim());
        }
    }
}
