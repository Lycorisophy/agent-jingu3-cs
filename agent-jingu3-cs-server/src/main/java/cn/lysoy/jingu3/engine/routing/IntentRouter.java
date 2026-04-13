package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ActionModePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 指南 §2 意图与模式选型在工程中的落点：三源路由 <strong>客户端显式 &gt; 关键词规则 &gt; LLM 分类</strong>。
 * 与「决策树」文档互补——此处为运行时实现。
 * <p>显式 mode 无法解析时降级为 {@link ActionMode#ASK}，来源 {@link RoutingSource#FALLBACK}。</p>
 * <p>显式 mode 解析成功但非对话可选时抛 {@link cn.lysoy.jingu3.common.exception.ServiceException}。</p>
 */
@Slf4j
@Component
public class IntentRouter {

    private final RuleBasedModeRouter rules;
    private final ModelIntentClassifier classifier;

    public IntentRouter(RuleBasedModeRouter rules, ModelIntentClassifier classifier) {
        this.rules = rules;
        this.classifier = classifier;
    }

    /**
     * 决定本轮 {@link ActionMode} 及路由来源。
     *
     * @param userMessage    用户自然语言，供规则与模型分类
     * @param modeFromClient 可空；非空时视为客户端显式指定（须为对话可选模式）
     * @return 永不为 {@code null}；非法显式 mode 时模式为 ASK、来源为 FALLBACK
     */
    public RoutingDecision resolve(String userMessage, String modeFromClient) {
        String msg = userMessage == null ? "" : userMessage;
        if (modeFromClient != null && !modeFromClient.isBlank()) {
            try {
                ActionMode mode = ActionMode.fromFlexibleName(modeFromClient);
                ActionModePolicy.assertConversationSelectable(mode);
                return new RoutingDecision(mode, RoutingSource.CLIENT_EXPLICIT, "payload.mode");
            } catch (IllegalArgumentException ex) {
                log.warn("invalid explicit mode [{}], fallback ASK", modeFromClient);
                return new RoutingDecision(ActionMode.ASK, RoutingSource.FALLBACK, "invalid_explicit_mode");
            }
        }
        return rules.route(msg)
                .map(m -> new RoutingDecision(m, RoutingSource.RULE, "keyword"))
                .orElseGet(() -> {
                    ActionMode m = classifier.classify(msg);
                    return new RoutingDecision(m, RoutingSource.MODEL, "llm");
                });
    }
}
