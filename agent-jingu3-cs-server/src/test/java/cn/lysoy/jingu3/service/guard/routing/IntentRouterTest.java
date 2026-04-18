package cn.lysoy.jingu3.service.guard.routing;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.guard.ActionMode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 三源路由单元测试（模型源使用 Mock）。
 */
class IntentRouterTest {

    private ModelIntentClassifier classifier;
    private IntentRouter router;

    @BeforeEach
    void setUp() {
        classifier = Mockito.mock(ModelIntentClassifier.class);
        Mockito.when(classifier.classify(Mockito.anyString())).thenReturn(ActionMode.REACT);
        Mockito.when(classifier.classifyOptional(Mockito.anyString()))
                .thenReturn(Optional.of(ActionMode.PLAN_AND_EXECUTE));
        router = new IntentRouter(new RuleBasedModeRouter(), classifier, new Jingu3Properties());
    }

    @Test
    void clientExplicit_hasHighestPriority() {
        RoutingDecision d = router.resolve("任意内容", "ASK");
        assertThat(d.getMode()).isEqualTo(ActionMode.ASK);
        assertThat(d.getSource()).isEqualTo(RoutingSource.CLIENT_EXPLICIT);
        Mockito.verifyNoInteractions(classifier);
    }

    @Test
    void rule_matchesBeforeModel() {
        RoutingDecision d = router.resolve("请帮我做计划任务", null);
        assertThat(d.getMode()).isEqualTo(ActionMode.PLAN_AND_EXECUTE);
        assertThat(d.getSource()).isEqualTo(RoutingSource.RULE);
        Mockito.verifyNoInteractions(classifier);
    }

    @Test
    void model_usedWhenNoRule() {
        RoutingDecision d = router.resolve("没有关键词的普通问题", null);
        assertThat(d.getSource()).isEqualTo(RoutingSource.MODEL);
        Mockito.verify(classifier).classify("没有关键词的普通问题");
    }

    @Test
    void invalidExplicitMode_fallsBackToAsk() {
        RoutingDecision d = router.resolve("x", "NOT_A_REAL_MODE");
        assertThat(d.getMode()).isEqualTo(ActionMode.ASK);
        assertThat(d.getSource()).isEqualTo(RoutingSource.FALLBACK);
        Mockito.verifyNoInteractions(classifier);
    }

    @Test
    void explicitCron_notConversationSelectable_throws() {
        assertThatThrownBy(() -> router.resolve("x", "CRON"))
                .isInstanceOf(ServiceException.class);
        Mockito.verifyNoInteractions(classifier);
    }

    @Test
    void explicitPlanAndExecute_classifierInfersAsk_downgradesToAskWithGuard() {
        Mockito.when(classifier.classifyOptional("你好")).thenReturn(Optional.of(ActionMode.ASK));
        RoutingDecision d = router.resolve("你好", "PLAN_AND_EXECUTE");
        assertThat(d.getMode()).isEqualTo(ActionMode.ASK);
        assertThat(d.getSource()).isEqualTo(RoutingSource.EXPLICIT_GUARD);
        assertThat(d.getGuardUserNotice()).isNotNull().contains("计划执行");
        Mockito.verify(classifier).classifyOptional("你好");
    }

    @Test
    void explicitPlanAndExecute_classifierFails_honorsExplicit() {
        Mockito.when(classifier.classifyOptional("长任务说明")).thenReturn(Optional.empty());
        RoutingDecision d = router.resolve("长任务说明", "PLAN_AND_EXECUTE");
        assertThat(d.getMode()).isEqualTo(ActionMode.PLAN_AND_EXECUTE);
        assertThat(d.getSource()).isEqualTo(RoutingSource.CLIENT_EXPLICIT);
        assertThat(d.getGuardUserNotice()).isNull();
    }

    @Test
    void explicitAgentTeam_classifierInfersReact_downgradesToAskWithGuard() {
        Mockito.when(classifier.classifyOptional("嗯")).thenReturn(Optional.of(ActionMode.REACT));
        RoutingDecision d = router.resolve("嗯", "AGENT_TEAM");
        assertThat(d.getMode()).isEqualTo(ActionMode.ASK);
        assertThat(d.getSource()).isEqualTo(RoutingSource.EXPLICIT_GUARD);
        assertThat(d.getGuardUserNotice()).contains("智能体团队");
    }

    @Test
    void explicitModeGuardDisabled_skipsClassifierAndHonorsExplicitPlan() {
        Jingu3Properties props = new Jingu3Properties();
        props.getRouting().setExplicitModeGuardEnabled(false);
        IntentRouter r = new IntentRouter(new RuleBasedModeRouter(), classifier, props);
        Mockito.when(classifier.classifyOptional("你好")).thenReturn(Optional.of(ActionMode.ASK));
        RoutingDecision d = r.resolve("你好", "PLAN_AND_EXECUTE");
        assertThat(d.getMode()).isEqualTo(ActionMode.PLAN_AND_EXECUTE);
        assertThat(d.getSource()).isEqualTo(RoutingSource.CLIENT_EXPLICIT);
        assertThat(d.getGuardUserNotice()).isNull();
        Mockito.verify(classifier, Mockito.never()).classifyOptional(Mockito.anyString());
    }
}
