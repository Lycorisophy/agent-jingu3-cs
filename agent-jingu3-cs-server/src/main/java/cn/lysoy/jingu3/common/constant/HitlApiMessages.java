package cn.lysoy.jingu3.common.constant;

/**
 * HITL 审批 API 的 HTTP 错误原因短语（与 {@link cn.lysoy.jingu3.service.hitl.HitlApprovalService} 对齐）。
 */
public final class HitlApiMessages {

    private HitlApiMessages() {
    }

    public static final String CONVERSATION_ID_REQUIRED = "conversationId 必填";
    public static final String APPROVAL_NOT_FOUND = "审批单不存在";
    public static final String APPROVAL_ALREADY_RESOLVED = "审批单已处理";
}
