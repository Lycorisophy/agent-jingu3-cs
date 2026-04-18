package cn.lysoy.jingu3.common.constant;

/**
 * {@link cn.lysoy.jingu3.service.guard.routing.RoutingDecision#getNote()} 中使用的键，便于检索与测试断言。
 */
public final class RoutingNotes {

    /** 客户端显式 mode 字符串无法解析为枚举 */
    public static final String INVALID_EXPLICIT_MODE = "invalid_explicit_mode";

    /** 路由备注后缀：WORKFLOW 无 workflowId 时已回落 ASK */
    public static final String WORKFLOW_ID_MISSING_FALLBACK_ASK_SUFFIX = "workflow_id_missing_fallback_ask";

    /** 显式 Plan/Agent Team 经守门降为 Ask */
    public static final String EXPLICIT_MODE_GUARD = "explicit_mode_guard";

    private RoutingNotes() {
    }
}
