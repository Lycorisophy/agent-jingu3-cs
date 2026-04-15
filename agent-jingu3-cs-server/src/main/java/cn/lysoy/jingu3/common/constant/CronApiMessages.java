package cn.lysoy.jingu3.common.constant;

/**
 * 定时任务 API 的 HTTP 错误原因短语（与 {@link cn.lysoy.jingu3.service.cron.ScheduledTaskService} 对齐）。
 */
public final class CronApiMessages {

    private CronApiMessages() {
    }

    public static final String CONVERSATION_SCOPE_REQUIRES_CONVERSATION_ID = "CONVERSATION 任务必须提供 conversationId";
    public static final String GLOBAL_SCOPE_SHOULD_NOT_SET_CONVERSATION_ID = "GLOBAL 任务不应携带 conversationId";
    public static final String PAYLOAD_SERIALIZE_FAILED = "payload 序列化失败";
}
