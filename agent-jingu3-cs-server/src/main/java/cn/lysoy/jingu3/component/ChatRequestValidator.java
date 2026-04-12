package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ActionModePolicy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天请求业务校验：对话可选模式、WORKFLOW 与 workflowId。
 */
@Component
public class ChatRequestValidator {

    public void validate(ChatRequest request) {
        if (request.getModePlan() != null && !request.getModePlan().isEmpty()) {
            validateModePlanTokens(request.getModePlan());
            if (modePlanContainsWorkflow(request.getModePlan()) && isBlank(request.getWorkflowId())) {
                throw new ServiceException(ErrorCode.BAD_REQUEST, EngineMessages.WORKFLOW_ID_REQUIRED);
            }
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

    /**
     * 路由决策为 WORKFLOW 时校验 workflowId。
     */
    public void requireWorkflowIdIfWorkflowMode(ActionMode mode, ChatRequest request) {
        if (mode == ActionMode.WORKFLOW && isBlank(request.getWorkflowId())) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, EngineMessages.WORKFLOW_ID_REQUIRED);
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

    private static boolean modePlanContainsWorkflow(List<String> tokens) {
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                if (ActionMode.fromFlexibleName(token) == ActionMode.WORKFLOW) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                // ignore
            }
        }
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
