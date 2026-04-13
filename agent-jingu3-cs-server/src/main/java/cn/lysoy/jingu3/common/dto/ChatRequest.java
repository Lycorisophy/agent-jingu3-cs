package cn.lysoy.jingu3.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 聊天请求体（REST）。
 * <p>若 {@code modePlan} 非空，优先于单字段 {@code mode}，按序编排执行。</p>
 * <p>选用 {@link cn.lysoy.jingu3.engine.ActionMode#WORKFLOW} 时建议传 {@code workflowId}；未传则回落 ASK（见规范）。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRequest {

    @NotBlank
    private String message;
    /** 可选：显式行动模式，如 ASK、REACT */
    private String mode;
    /** 可选：会话标识 */
    private String conversationId;
    /** 可选：多步模式名（顺序执行）；非空时优先于 {@code mode} */
    private List<String> modePlan;
    /** WORKFLOW 模式或编排含 WORKFLOW 时必填 */
    private String workflowId;

    /**
     * 可选：请求 ID；与请求头 {@code X-Request-Id} 二选一或同时提供，体中非空时覆盖 MDC 中的值。
     */
    private String requestId;

    /**
     * 可选：追踪 ID；与请求头 {@code X-Trace-Id} 二选一或同时提供，体中非空时覆盖 MDC 中的值。
     */
    private String traceId;

    /**
     * 可选：客户端平台标识（如 web、ios）；HTTP 可与请求头 {@code X-Jingu3-Client-Platform} 二选一，握手头优先；
     * 浏览器 WebSocket 无法自定义握手头时可仅传本字段。
     */
    private String clientPlatform;
}
