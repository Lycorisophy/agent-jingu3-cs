package cn.lysoy.jingu3.common.constant;

/**
 * 聊天 HTTP / 流式协议中与客户端约定的展示用常量（非枚举名业务值）。
 */
public final class ChatApiConstants {

    /**
     * SSE/WebSocket 流式 {@code meta} 事件中，编排 {@code modePlan} 路径下 {@code actionMode} 字段的占位展示值。
     */
    public static final String STREAM_META_ACTION_MODE_MODE_PLAN = "MODE_PLAN";

    private ChatApiConstants() {
    }
}
