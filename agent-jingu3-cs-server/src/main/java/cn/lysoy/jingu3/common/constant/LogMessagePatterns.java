package cn.lysoy.jingu3.common.constant;

/**
 * SLF4J 占位日志文案（与 {@link org.slf4j.Logger#warn(String, Object...)} 占位符一致）。
 * <p>避免日志字符串重复，便于 Sonar 与 Qodana 维护性检查。</p>
 */
public final class LogMessagePatterns {

    public static final String INTENT_CLASSIFIER_UNPARSEABLE_OUTPUT =
            "无法解析模型输出 [{}]，降级为 ASK";

    public static final String INTENT_CLASSIFIER_FAILED = "模型意图分类失败，降级为 ASK: {}";

    /** 与 {@link cn.lysoy.jingu3.service.guard.routing.RoutingFallbacks#modePlanStepOrAskIfWorkflowWithoutId} 日志一致 */
    public static final String MODE_PLAN_WORKFLOW_WITHOUT_ID_FALLBACK_ASK =
            "modePlan 步骤为 WORKFLOW 但未提供 workflowId，回落 ASK";

    private LogMessagePatterns() {
    }
}
