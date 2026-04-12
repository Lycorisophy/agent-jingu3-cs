package cn.lysoy.jingu3.common.constant;

/**
 * 引擎层对外/对内文案与校验提示（非 LLM 提示词）。
 */
public final class EngineMessages {

    public static final String MODE_CANNOT_BE_BLANK = "mode 不能为空";

    /** 参数为模式枚举名；用于对话 API 禁止选取 CRON / STATE_TRACKING / HUMAN_IN_LOOP。 */
    public static final String MODE_NOT_CONVERSATION_SELECTABLE =
            "模式 %s 不能通过对话接口选取（定时任务请用独立模块或技能；状态追踪/人在环为其它模式的辅助能力）";

    public static final String WORKFLOW_ID_REQUIRED =
            "选择 WORKFLOW 模式或编排中含 WORKFLOW 步骤时必须提供非空 workflowId";

    /**
     * Stub 占位回复模板：参数依次为 mode、username、userId。
     */
    public static final String STUB_REPLY_TEMPLATE =
            "[v0.1 Stub] 模式 %s 尚未实现业务闭环，请等待后续版本。用户=%s(%s)";

    /**
     * Cron MVP：参数依次为演示 cron 表达式、用户原始诉求摘要。
     */
    public static final String CRON_DEMO_REPLY =
            "[Cron MVP] 已记录定时意图。演示调度表达式：%s。用户诉求摘要：%s";

    /** 人在环 MVP：固定说明。 */
    public static final String HUMAN_IN_LOOP_PENDING =
            "[人在环 MVP] 该操作需人工确认后才能继续执行；当前为演示环境，未接入真实审批流。";

    /**
     * 状态追踪 MVP：参数依次为 conversationId、累计步数。
     */
    public static final String STATE_TRACKING_REPLY =
            "[状态追踪 MVP] 会话=%s，本模式累计交互次数=%d（进程内演示，重启清零）。";

    private EngineMessages() {
    }
}
