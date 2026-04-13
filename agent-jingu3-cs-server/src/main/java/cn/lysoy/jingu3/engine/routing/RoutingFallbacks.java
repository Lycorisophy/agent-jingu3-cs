package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.common.constant.LogMessagePatterns;
import cn.lysoy.jingu3.common.constant.RoutingNotes;
import cn.lysoy.jingu3.engine.ActionMode;
import lombok.extern.slf4j.Slf4j;

/**
 * 路由层统一回落策略：在无法按原模式安全执行时改为 {@link ActionMode#ASK}。
 */
@Slf4j
public final class RoutingFallbacks {

    private RoutingFallbacks() {
    }

    /**
     * {@code modePlan} 单步解析为 WORKFLOW 但未提供 {@code workflowId} 时，该步按 ASK 执行。
     */
    public static ActionMode modePlanStepOrAskIfWorkflowWithoutId(ActionMode parsed, String workflowId) {
        if (parsed != ActionMode.WORKFLOW) {
            return parsed;
        }
        if (workflowId != null && !workflowId.isBlank()) {
            return parsed;
        }
        log.warn(LogMessagePatterns.MODE_PLAN_WORKFLOW_WITHOUT_ID_FALLBACK_ASK);
        return ActionMode.ASK;
    }

    /**
     * 选用工作流模式但未提供 {@code workflowId} 时，回落为单轮问答（ASK），避免抛错。
     *
     * @param decision     意图路由结果
     * @param workflowId   请求中的工作流 id，可空
     * @return 若原为 WORKFLOW 且 id 为空/空白，则返回 ASK 决策；否则原样返回
     */
    public static RoutingDecision askIfWorkflowWithoutWorkflowId(RoutingDecision decision, String workflowId) {
        if (decision.getMode() != ActionMode.WORKFLOW) {
            return decision;
        }
        if (workflowId != null && !workflowId.isBlank()) {
            return decision;
        }
        String base = decision.getNote();
        String note =
                (base == null || base.isBlank())
                        ? RoutingNotes.WORKFLOW_ID_MISSING_FALLBACK_ASK_SUFFIX
                        : base + ";" + RoutingNotes.WORKFLOW_ID_MISSING_FALLBACK_ASK_SUFFIX;
        return new RoutingDecision(ActionMode.ASK, decision.getSource(), note);
    }
}
