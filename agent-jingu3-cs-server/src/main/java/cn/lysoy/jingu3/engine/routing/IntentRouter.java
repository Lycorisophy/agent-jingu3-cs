package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.common.constant.RoutingNotes;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ActionModePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 指南 §2 意图与模式选型在工程中的落点：三源路由 <strong>客户端显式 &gt; 关键词规则 &gt; LLM 分类</strong>。
 * 与「决策树」文档互补——此处为运行时实现。
 * <p>显式 mode 无法解析时降级为 {@link ActionMode#ASK}，来源 {@link RoutingSource#FALLBACK}。</p>
 * <p>显式 mode 解析成功但非对话可选时抛 {@link cn.lysoy.jingu3.common.exception.ServiceException}。</p>
 * <p>显式 {@link ActionMode#PLAN_AND_EXECUTE} / {@link ActionMode#AGENT_TEAM} 时追加守门：模型分类为 ASK/REACT 则降为 ASK，
 * 来源 {@link RoutingSource#EXPLICIT_GUARD}；分类器调用失败（{@link ModelIntentClassifier#classifyOptional} empty）时尊重显式选择。</p>
 * <p>可通过 {@code jingu3.routing.explicit-mode-guard-enabled=false} 关闭守门。</p>
 */
@Slf4j
@Component
public class IntentRouter {

    private final RuleBasedModeRouter rules;
    private final ModelIntentClassifier classifier;
    private final Jingu3Properties properties;

    public IntentRouter(
            RuleBasedModeRouter rules, ModelIntentClassifier classifier, Jingu3Properties properties) {
        this.rules = rules;
        this.classifier = classifier;
        this.properties = properties;
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
                if (properties.getRouting().isExplicitModeGuardEnabled()
                        && (mode == ActionMode.PLAN_AND_EXECUTE || mode == ActionMode.AGENT_TEAM)) {
                    Optional<ActionMode> inferredOpt = classifier.classifyOptional(msg);
                    if (inferredOpt.isPresent()) {
                        ActionMode inferred = inferredOpt.get();
                        if (inferred == ActionMode.ASK || inferred == ActionMode.REACT) {
                            String label =
                                    mode == ActionMode.PLAN_AND_EXECUTE ? "计划执行" : "智能体团队";
                            String notice = String.format(EngineMessages.EXPLICIT_MODE_GUARD_NOTICE, label);
                            log.info(
                                    "explicit mode guard: requested={} inferred={} -> ASK",
                                    mode,
                                    inferred);
                            return new RoutingDecision(
                                    ActionMode.ASK,
                                    RoutingSource.EXPLICIT_GUARD,
                                    RoutingNotes.EXPLICIT_MODE_GUARD,
                                    notice);
                        }
                    }
                }
                return new RoutingDecision(mode, RoutingSource.CLIENT_EXPLICIT, "payload.mode");
            } catch (IllegalArgumentException ex) {
                log.warn("invalid explicit mode [{}], fallback ASK", modeFromClient);
                return new RoutingDecision(ActionMode.ASK, RoutingSource.FALLBACK, RoutingNotes.INVALID_EXPLICIT_MODE);
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
