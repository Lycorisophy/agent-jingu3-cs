package cn.lysoy.jingu3.service;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.component.ChatRequestValidator;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.vo.ChatVo;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.ModeRegistry;
import cn.lysoy.jingu3.engine.orchestration.ModePlanExecutor;
import cn.lysoy.jingu3.engine.routing.IntentRouter;
import cn.lysoy.jingu3.engine.routing.RoutingDecision;
import cn.lysoy.jingu3.engine.routing.RoutingFallbacks;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 同步对话编排：校验 →（可选）多步 modePlan → 否则三源意图路由 → 构造 {@link ExecutionContext} →
 * {@link ModeRegistry#get(ActionMode)} 执行阻塞式 {@link cn.lysoy.jingu3.engine.ActionModeHandler#execute}。
 * <p>流式输出见 {@link ChatStreamService}，与本类共享校验与路由逻辑。</p>
 */
@Slf4j
@Service
public class ChatService {

    private final IntentRouter intentRouter;
    private final ModeRegistry modeRegistry;
    private final UserConstants userConstants;
    private final ModePlanExecutor modePlanExecutor;
    private final ChatRequestValidator chatRequestValidator;

    public ChatService(
            IntentRouter intentRouter,
            ModeRegistry modeRegistry,
            UserConstants userConstants,
            ModePlanExecutor modePlanExecutor,
            ChatRequestValidator chatRequestValidator) {
        this.intentRouter = intentRouter;
        this.modeRegistry = modeRegistry;
        this.userConstants = userConstants;
        this.modePlanExecutor = modePlanExecutor;
        this.chatRequestValidator = chatRequestValidator;
    }

    /**
     * 处理一轮聊天请求，返回完整 {@link ChatVo}（非流式）。
     *
     * @param request 含 message、可选 mode / modePlan / workflowId 等
     * @return 聚合后的回复与路由元数据
     */
    public ChatVo chat(ChatRequest request) {
        chatRequestValidator.validate(request);

        // 指南 §11：客户端显式编排优先于单模式路由
        if (request.getModePlan() != null && !request.getModePlan().isEmpty()) {
            ChatVo vo = modePlanExecutor.execute(request, userConstants);
            log.info("modePlan userId={} steps={} lastMode={} conv={}",
                    userConstants.getId(),
                    vo.getPlanSteps() != null ? vo.getPlanSteps().size() : 0,
                    vo.getActionMode(),
                    request.getConversationId());
            return vo;
        }

        RoutingDecision decision =
                RoutingFallbacks.askIfWorkflowWithoutWorkflowId(
                        intentRouter.resolve(request.getMessage(), request.getMode()), request.getWorkflowId());

        log.info("routing userId={} source={} mode={} note={} conv={}",
                userConstants.getId(),
                decision.getSource(),
                decision.getMode(),
                decision.getNote(),
                request.getConversationId());

        ExecutionContext ctx = new ExecutionContext(
                userConstants.getId(),
                userConstants.getUsername(),
                request.getConversationId() == null || request.getConversationId().isBlank()
                        ? ConversationConstants.DEFAULT_CONVERSATION_ID
                        : request.getConversationId(),
                request.getMessage(),
                decision.getMode(),
                decision.getSource(),
                List.of(),
                null,
                decision.getMode() == ActionMode.WORKFLOW ? request.getWorkflowId() : null
        );
        String reply = modeRegistry.get(decision.getMode()).execute(ctx);
        ChatVo vo = new ChatVo();
        vo.setUserId(userConstants.getId());
        vo.setUsername(userConstants.getUsername());
        vo.setReply(reply);
        vo.setActionMode(decision.getMode().name());
        vo.setRoutingSource(decision.getSource().name());
        vo.setPlanSteps(null);
        return vo;
    }
}
