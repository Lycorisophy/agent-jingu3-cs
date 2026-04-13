package cn.lysoy.jingu3.common.constant;

/**
 * 会话相关字面量（缺省 conversationId 等），避免魔法字符串。
 */
public final class ConversationConstants {

    /** 请求未带 conversationId 时使用的占位会话标识 */
    public static final String DEFAULT_CONVERSATION_ID = "default";

    private ConversationConstants() {
    }
}
