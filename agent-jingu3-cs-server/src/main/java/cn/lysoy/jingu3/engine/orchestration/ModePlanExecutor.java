package cn.lysoy.jingu3.engine.orchestration;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.vo.ChatVo;
import cn.lysoy.jingu3.common.vo.PlanStepVo;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.ModeRegistry;
import cn.lysoy.jingu3.engine.routing.RoutingFallbacks;
import cn.lysoy.jingu3.engine.routing.RoutingSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 指南 §11 模式组合的一种实现：按客户端 {@link ChatRequest#getModePlan()} 声明的顺序，依次执行多种
 * {@link ActionMode}，步间通过 {@link PromptFragments#PLAN_CHAIN_SEPARATOR} 把用户原话与上一步答复拼进
 * {@link ExecutionContext#getTaskPayload()}，使下一步模型能「看见」链式上下文。
 * <p>仅顺序执行；并行 DAG 未在本类实现。</p>
 */
@Slf4j
@Component
public class ModePlanExecutor {

    /** 防止过长编排耗尽资源 */
    public static final int MAX_STEPS = 8;

    private final ModeRegistry modeRegistry;

    public ModePlanExecutor(ModeRegistry modeRegistry) {
        this.modeRegistry = modeRegistry;
    }

    /**
     * 执行完整编排并返回最后一步的答复及每步明细。
     *
     * @param request 须含非空 {@code modePlan}
     * @param users   当前用户信息（来自配置或鉴权）
     */
    public ChatVo execute(ChatRequest request, UserConstants users) {
        List<String> raw = request.getModePlan();
        if (raw == null || raw.isEmpty()) {
            throw new IllegalStateException("modePlan required");
        }
        if (raw.size() > MAX_STEPS) {
            log.warn("modePlan truncated from {} to {}", raw.size(), MAX_STEPS);
            raw = raw.subList(0, MAX_STEPS);
        }
        String conv = request.getConversationId() == null || request.getConversationId().isBlank()
                ? ConversationConstants.DEFAULT_CONVERSATION_ID
                : request.getConversationId();
        List<PlanStepVo> steps = new ArrayList<>();
        String chainPayload = null;
        ActionMode lastMode = ActionMode.ASK;
        for (String token : raw) {
            ActionMode mode =
                    RoutingFallbacks.modePlanStepOrAskIfWorkflowWithoutId(parseOrReact(token), request.getWorkflowId());
            lastMode = mode;
            String workflowId = mode == ActionMode.WORKFLOW ? request.getWorkflowId() : null;
            ExecutionContext ctx = new ExecutionContext(
                    users.getId(),
                    users.getUsername(),
                    conv,
                    request.getMessage(),
                    mode,
                    RoutingSource.CLIENT_EXPLICIT,
                    List.of(),
                    chainPayload,
                    workflowId
            );
            String reply = modeRegistry.get(mode).execute(ctx);
            steps.add(new PlanStepVo(mode.name(), reply));
            chainPayload = request.getMessage() + PromptFragments.PLAN_CHAIN_SEPARATOR + reply;
        }
        String finalReply = steps.isEmpty() ? "" : steps.get(steps.size() - 1).getReply();
        ChatVo vo = new ChatVo();
        vo.setUserId(users.getId());
        vo.setUsername(users.getUsername());
        vo.setReply(finalReply);
        vo.setActionMode(lastMode.name());
        vo.setRoutingSource(RoutingSource.CLIENT_EXPLICIT.name());
        vo.setPlanSteps(steps);
        return vo;
    }

    /**
     * 非法或空白 token 降级为 REACT，与 {@link cn.lysoy.jingu3.service.ChatStreamService} 中逻辑一致。
     */
    private static ActionMode parseOrReact(String raw) {
        if (raw == null || raw.isBlank()) {
            return ActionMode.REACT;
        }
        try {
            return ActionMode.fromFlexibleName(raw);
        } catch (IllegalArgumentException ex) {
            return ActionMode.REACT;
        }
    }

}
