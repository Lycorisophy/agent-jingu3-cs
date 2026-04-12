package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.common.constant.RoutingRuleKeywords;
import cn.lysoy.jingu3.engine.ActionMode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 基于关键词的简单规则路由（v0.1）；后续可换配置表或脚本引擎。
 */
@Component
public class RuleBasedModeRouter {

    /** 有序匹配：先匹配先生效 */
    private final Map<String, ActionMode> keywordToMode;

    public RuleBasedModeRouter() {
        keywordToMode = new LinkedHashMap<>();
        keywordToMode.put(RoutingRuleKeywords.PLAN, ActionMode.PLAN_AND_EXECUTE);
        keywordToMode.put(RoutingRuleKeywords.WORKFLOW, ActionMode.WORKFLOW);
        keywordToMode.put(RoutingRuleKeywords.CRON, ActionMode.CRON);
        keywordToMode.put(RoutingRuleKeywords.STATE, ActionMode.STATE_TRACKING);
        keywordToMode.put(RoutingRuleKeywords.HUMAN_LOOP, ActionMode.HUMAN_IN_LOOP);
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
