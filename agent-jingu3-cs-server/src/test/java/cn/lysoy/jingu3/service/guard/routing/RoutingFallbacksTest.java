package cn.lysoy.jingu3.service.guard.routing;

import cn.lysoy.jingu3.service.guard.ActionMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingFallbacksTest {

    @Test
    void workflowWithoutId_becomesAsk() {
        RoutingDecision in = new RoutingDecision(ActionMode.WORKFLOW, RoutingSource.RULE, "keyword");
        RoutingDecision out = RoutingFallbacks.askIfWorkflowWithoutWorkflowId(in, null);
        assertThat(out.getMode()).isEqualTo(ActionMode.ASK);
        assertThat(out.getNote()).contains("workflow_id_missing_fallback_ask");
    }

    @Test
    void workflowWithId_unchanged() {
        RoutingDecision in = new RoutingDecision(ActionMode.WORKFLOW, RoutingSource.CLIENT_EXPLICIT, "x");
        RoutingDecision out = RoutingFallbacks.askIfWorkflowWithoutWorkflowId(in, "wf-1");
        assertThat(out).isSameAs(in);
    }

    @Test
    void modePlanStep_workflowWithoutId_ask() {
        assertThat(RoutingFallbacks.modePlanStepOrAskIfWorkflowWithoutId(ActionMode.WORKFLOW, ""))
                .isEqualTo(ActionMode.ASK);
        assertThat(RoutingFallbacks.modePlanStepOrAskIfWorkflowWithoutId(ActionMode.WORKFLOW, "id"))
                .isEqualTo(ActionMode.WORKFLOW);
    }
}
