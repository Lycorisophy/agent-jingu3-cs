package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.engine.ActionMode;
import org.springframework.stereotype.Component;

/**
 * 三源路由：客户端显式 &gt; 规则 &gt; 模型。
 */
@Component
public class IntentRouter {

    private final RuleBasedModeRouter rules;
    private final ModelIntentClassifier classifier;

    public IntentRouter(RuleBasedModeRouter rules, ModelIntentClassifier classifier) {
        this.rules = rules;
        this.classifier = classifier;
    }

    /**
     * @param modeFromClient 可空；非空时视为客户端显式指定
     */
    public RoutingDecision resolve(String userMessage, String modeFromClient) {
        String msg = userMessage == null ? "" : userMessage;
        if (modeFromClient != null && !modeFromClient.isBlank()) {
            ActionMode mode = ActionMode.fromFlexibleName(modeFromClient);
            return new RoutingDecision(mode, RoutingSource.CLIENT_EXPLICIT, "payload.mode");
        }
        return rules.route(msg)
                .map(m -> new RoutingDecision(m, RoutingSource.RULE, "keyword"))
                .orElseGet(() -> {
                    ActionMode m = classifier.classify(msg);
                    return new RoutingDecision(m, RoutingSource.MODEL, "llm");
                });
    }
}
