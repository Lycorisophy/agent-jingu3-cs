package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        router = new IntentRouter(new RuleBasedModeRouter(), classifier);
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
    void invalidExplicitMode_fallsBackToReact() {
        RoutingDecision d = router.resolve("x", "NOT_A_REAL_MODE");
        assertThat(d.getMode()).isEqualTo(ActionMode.REACT);
        assertThat(d.getSource()).isEqualTo(RoutingSource.FALLBACK);
        Mockito.verifyNoInteractions(classifier);
    }

    @Test
    void explicitCron_notConversationSelectable_throws() {
        assertThatThrownBy(() -> router.resolve("x", "CRON"))
                .isInstanceOf(ServiceException.class);
        Mockito.verifyNoInteractions(classifier);
    }
}
