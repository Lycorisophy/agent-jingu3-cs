package cn.lysoy.jingu3.common.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 聊天请求体（v0.1 REST）。
 */
public record ChatRequest(
        @NotBlank String message,
        /** 可选：显式行动模式，如 ASK、REACT */
        String mode,
        /** 可选：会话标识，便于后续记忆史诗 */
        String conversationId
) {
}
