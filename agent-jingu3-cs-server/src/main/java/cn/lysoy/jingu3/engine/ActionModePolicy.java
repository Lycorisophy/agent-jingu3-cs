package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.common.constant.EngineMessages;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 行动模式在「<strong>对话 HTTP/WebSocket API</strong>」层的<strong>可见性策略</strong>（产品驾驭边界）：
 * 指南中八种模式在引擎内均有 {@link cn.lysoy.jingu3.engine.ActionModeHandler} 实现，但终端用户通过 {@code mode} /
 * {@code modePlan} 仅允许选取其中五种（ASK、REACT、PLAN_AND_EXECUTE、WORKFLOW、AGENT_TEAM），避免将
 * CRON、STATE_TRACKING、HUMAN_IN_LOOP 当作普通对话模式误选（与指南 §8/§9/§10 及路线图 Cron/HITL 独立入口一致）。
 */
public final class ActionModePolicy {

    /** 用户可通过 /chat 显式传入或由意图路由落入的模式集合（与错误提示文案 {@link cn.lysoy.jingu3.common.constant.EngineMessages#MODE_NOT_CONVERSATION_SELECTABLE} 对应） */
    private static final Set<ActionMode> CONVERSATION_SELECTABLE = Set.of(
            ActionMode.ASK,
            ActionMode.REACT,
            ActionMode.PLAN_AND_EXECUTE,
            ActionMode.WORKFLOW,
            ActionMode.AGENT_TEAM
    );

    private ActionModePolicy() {
    }

    /**
     * @param mode 待检查的模式
     * @return 是否允许作为对话接口的可选模式
     */
    public static boolean isConversationSelectable(ActionMode mode) {
        return mode != null && CONVERSATION_SELECTABLE.contains(mode);
    }

    /**
     * 若模式不允许通过对话接口选取，则抛出业务异常（用于校验请求体中的 {@code mode}）。
     *
     * @throws ServiceException 当模式不能通过对话接口选取时
     */
    public static void assertConversationSelectable(ActionMode mode) {
        if (!isConversationSelectable(mode)) {
            throw new ServiceException(
                    ErrorCode.BAD_REQUEST,
                    String.format(EngineMessages.MODE_NOT_CONVERSATION_SELECTABLE, mode.name()));
        }
    }

    /**
     * 供 {@link cn.lysoy.jingu3.engine.routing.ModelIntentClassifier} 拼接系统提示，约束模型只输出允许的模式名。
     *
     * @return 逗号分隔的大写枚举名列表
     */
    public static String conversationSelectableNamesJoined() {
        return CONVERSATION_SELECTABLE.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
