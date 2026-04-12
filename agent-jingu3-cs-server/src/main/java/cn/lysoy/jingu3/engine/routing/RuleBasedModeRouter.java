package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.common.constant.RoutingRuleKeywords;
import cn.lysoy.jingu3.engine.ActionMode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 基于关键词的简单规则路由。
 * <p>不映射 CRON / STATE_TRACKING / HUMAN_IN_LOOP（对话不可选，见 {@link cn.lysoy.jingu3.engine.ActionModePolicy}）。</p>
 */
@Component
public class RuleBasedModeRouter {

    /** 有序匹配：先匹配先生效 */
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
