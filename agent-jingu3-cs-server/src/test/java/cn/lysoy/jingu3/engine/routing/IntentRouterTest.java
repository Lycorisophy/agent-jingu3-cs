package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.engine.ActionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(d.mode()).isEqualTo(ActionMode.ASK);
        assertThat(d.source()).isEqualTo(RoutingSource.CLIENT_EXPLICIT);
        Mockito.verifyNoInteractions(classifier);
    }

    @Test
    void rule_matchesBeforeModel() {
        RoutingDecision d = router.resolve("请帮我做定时提醒", null);
        assertThat(d.mode()).isEqualTo(ActionMode.CRON);
        assertThat(d.source()).isEqualTo(RoutingSource.RULE);
        Mockito.verifyNoInteractions(classifier);
    }

    @Test
    void model_usedWhenNoRule() {
        RoutingDecision d = router.resolve("没有关键词的普通问题", null);
        assertThat(d.source()).isEqualTo(RoutingSource.MODEL);
        Mockito.verify(classifier).classify("没有关键词的普通问题");
    }
}
