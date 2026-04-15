package cn.lysoy.jingu3.common.constant;

/**
 * 对话状态（DST）API 的 HTTP 错误原因短语（与 {@link cn.lysoy.jingu3.service.dst.DialogueStateService} 对齐）。
 */
public final class DstApiMessages {

    private DstApiMessages() {
    }

    public static final String NO_STATE_FOR_CONVERSATION = "该会话尚无状态";
    public static final String REVISION_CONFLICT = "revision 不匹配，请刷新后重试";
}
