package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ActionModePolicy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天请求业务校验：对话可选模式。工作流无 {@code workflowId} 时由路由/编排回落为 ASK，不在此抛错。
 */
@Component
public class ChatRequestValidator {

    public void validate(ChatRequest request) {
        if (request.getModePlan() != null && !request.getModePlan().isEmpty()) {
            validateModePlanTokens(request.getModePlan());
            return;
        }
        if (!isBlank(request.getMode())) {
            try {
                ActionMode m = ActionMode.fromFlexibleName(request.getMode());
                ActionModePolicy.assertConversationSelectable(m);
            } catch (IllegalArgumentException ignored) {
                // 非法 mode 字符串交由 IntentRouter 降级
            }
        }
    }

    private static void validateModePlanTokens(List<String> tokens) {
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                ActionMode m = ActionMode.fromFlexibleName(token);
                ActionModePolicy.assertConversationSelectable(m);
            } catch (IllegalArgumentException ignored) {
                // 无法解析的步骤由 ModePlanExecutor 降级为 REACT
            }
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
