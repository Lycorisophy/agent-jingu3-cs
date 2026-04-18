package cn.lysoy.jingu3.service.mode.orchestration;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.vo.ChatVo;
import cn.lysoy.jingu3.common.vo.PlanStepVo;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.service.guard.ActionMode;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.guard.ModeRegistry;
import cn.lysoy.jingu3.service.guard.routing.RoutingFallbacks;
import cn.lysoy.jingu3.service.guard.routing.RoutingSource;
import cn.lysoy.jingu3.service.context.prepare.UserPromptPreparationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * <strong>模式组合 / 显式编排</strong>（指南 §11，驾驭工程）：按客户端 {@link ChatRequest#getModePlan()} 声明的
 * <strong>顺序列表</strong>，逐步执行多种 {@link ActionMode}；步间通过 {@link PromptFragments#PLAN_CHAIN_SEPARATOR}
 * 将「用户原问 + 上一步模型输出」写入 {@link ExecutionContext#getTaskPayload()}，下一步的 {@link ExecutionContext#llmInput()}
 * 即携带链式上下文，使不同模式在同一用户问题上形成<strong>流水线式推理</strong>。
 * <p>仅实现<strong>线性顺序</strong>；并行 DAG、条件分支未在本类覆盖（见路线图与工作流平台设计）。</p>
 */
@Slf4j
@Component
public class ModePlanExecutor {

    /** 防止过长编排耗尽资源 */
    public static final int MAX_STEPS = 8;

    /** 每步根据解析出的模式调用对应 {@link cn.lysoy.jingu3.service.mode.ActionModeHandler#execute} */
    private final ModeRegistry modeRegistry;

    /** 与对话主路径一致：每步使用同一「增强后」用户基线串 effectiveMessage */
    private final UserPromptPreparationService userPromptPreparationService;

    public ModePlanExecutor(ModeRegistry modeRegistry, UserPromptPreparationService userPromptPreparationService) {
        this.modeRegistry = modeRegistry;
        this.userPromptPreparationService = userPromptPreparationService;
    }

    /**
     * 执行完整编排并返回最后一步的答复及每步明细。
     *
     * @param request 须含非空 {@code modePlan}
     * @param users   当前用户信息（来自配置或鉴权）
     */
    public ChatVo execute(ChatRequest request, UserConstants users, Instant serverTimeUtc) {
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
        String effectiveMessage = userPromptPreparationService.prepare(request, users.getId(), serverTimeUtc);
        List<PlanStepVo> steps = new ArrayList<>();
        // 步间链式负载：首步为 null，之后每步为「用户原问 + 分隔符 + 上一步答复」
        String chainPayload = null;
        ActionMode lastMode = ActionMode.ASK;
        for (String token : raw) {
            // modePlan 每步 token 解析为枚举；WORKFLOW 且无 workflowId 时回落 ASK，避免非法状态
            ActionMode mode =
                    RoutingFallbacks.modePlanStepOrAskIfWorkflowWithoutId(parseOrReact(token), request.getWorkflowId());
            lastMode = mode;
            String workflowId = mode == ActionMode.WORKFLOW ? request.getWorkflowId() : null;
            ExecutionContext ctx = new ExecutionContext(
                    users.getId(),
                    users.getUsername(),
                    conv,
                    effectiveMessage,
                    mode,
                    RoutingSource.CLIENT_EXPLICIT,
                    List.of(),
                    chainPayload,
                    workflowId
            );
            String reply = modeRegistry.get(mode).execute(ctx);
            steps.add(new PlanStepVo(mode.name(), reply));
            chainPayload = effectiveMessage + PromptFragments.PLAN_CHAIN_SEPARATOR + reply;
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
     * 非法或空白 token 降级为 REACT，与 {@link cn.lysoy.jingu3.service.context.chat.ChatStreamService} 中逻辑一致。
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
