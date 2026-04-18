package cn.lysoy.jingu3.common.constant;

/**
 * 规则路由关键词（与 {@link cn.lysoy.jingu3.service.guard.routing.RuleBasedModeRouter} 顺序无关，由 Map 插入顺序决定优先级）。
 */
public final class RoutingRuleKeywords {

    public static final String PLAN = "计划";
    public static final String WORKFLOW = "工作流";
    public static final String CRON = "定时";
    public static final String STATE = "状态";
    public static final String HUMAN_LOOP = "人工";
    public static final String AGENT_TEAM = "团队";

    private RoutingRuleKeywords() {
    }
}
