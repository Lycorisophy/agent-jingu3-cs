package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.common.constant.RoutingRuleKeywords;
import cn.lysoy.jingu3.engine.ActionMode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * <strong>规则意图路由</strong>（指南 §2 三源中的第二源）：对用户自然语言做<strong>子串包含</strong>匹配，命中则返回对应
 * {@link ActionMode}；未命中返回 empty，交由 {@link ModelIntentClassifier}。关键词维护在 {@link RoutingRuleKeywords}，
 * 避免魔法字符串散落在业务代码中。
 * <p>不映射 CRON / STATE_TRACKING / HUMAN_IN_LOOP（对话不可选，见 {@link cn.lysoy.jingu3.engine.ActionModePolicy}）。</p>
 */
@Component
public class RuleBasedModeRouter {

    /** LinkedHashMap：有序匹配，先声明的关键词优先（如先匹配「计划」再匹配泛化词） */
    private final Map<String, ActionMode> keywordToMode;

    public RuleBasedModeRouter() {
        keywordToMode = new LinkedHashMap<>();
        keywordToMode.put(RoutingRuleKeywords.PLAN, ActionMode.PLAN_AND_EXECUTE);
        keywordToMode.put(RoutingRuleKeywords.WORKFLOW, ActionMode.WORKFLOW);
        keywordToMode.put(RoutingRuleKeywords.AGENT_TEAM, ActionMode.AGENT_TEAM);
    }

    /**
     * @return 未命中时 empty
     */
    public Optional<ActionMode> route(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }
        String text = userMessage;
        for (Map.Entry<String, ActionMode> e : keywordToMode.entrySet()) {
            if (text.contains(e.getKey())) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }
}
