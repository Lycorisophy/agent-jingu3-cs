package cn.lysoy.jingu3.common.vo;

import cn.lysoy.jingu3.common.dto.ChatRequest;

/**
 * 聊天接口出参（VO），与 {@link ChatRequest} 对应。
 */
public record ChatVo(
        String userId,
        String username,
        String reply,
        String actionMode,
        String routingSource
) {
}
