package cn.lysoy.jingu3.service.context.chat;

import cn.lysoy.jingu3.component.ChatInboundPlatformSupport;
import cn.lysoy.jingu3.component.ChatRequestValidator;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.vo.ChatVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.guard.ActionMode;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.guard.ModeRegistry;
import cn.lysoy.jingu3.service.mode.orchestration.ModePlanExecutor;
import cn.lysoy.jingu3.service.guard.routing.IntentRouter;
import cn.lysoy.jingu3.service.guard.routing.RoutingDecision;
import cn.lysoy.jingu3.service.guard.routing.RoutingFallbacks;
import cn.lysoy.jingu3.service.context.prepare.UserPromptPreparationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * <strong>同步对话编排入口</strong>（驾驭工程 + 上下文工程）：HTTP {@code POST /api/v1/chat} 的非流式路径。
 * <p><strong>管线顺序</strong>：平台头合并 → 请求校验 →（可选）用户句密文落库 →
 * 若带 {@code modePlan} 则走 {@link ModePlanExecutor}（多模式顺序执行）；否则经
 * {@link UserPromptPreparationService} 做纠正语与 UTC/平台等<strong>送模前用户串改写</strong> →
 * {@link IntentRouter} 三源路由并经 {@link RoutingFallbacks} 处理 WORKFLOW 无 id 等回落 →
 * 构造 {@link ExecutionContext} → {@link ModeRegistry} 选中 {@link cn.lysoy.jingu3.service.mode.ActionModeHandler#execute}。</p>
 * <p>流式输出见 {@link ChatStreamService}；二者应保持路由与上下文构造语义一致。</p>
 */
@Slf4j
@Service
public class ChatService {

    private final IntentRouter intentRouter;
    private final ModeRegistry modeRegistry;
    private final UserConstants userConstants;
    private final ModePlanExecutor modePlanExecutor;
    private final ChatRequestValidator chatRequestValidator;
    private final UserPromptPreparationService userPromptPreparationService;
    private final UserPromptCipherPersistenceService userPromptCipherPersistenceService;
    private final ChatStmHistoryAssembler chatStmHistoryAssembler;
    private final ConversationStmBuffer conversationStmBuffer;
    private final Jingu3Properties jingu3Properties;
    private final UserCorrectionMemoryPersistence userCorrectionMemoryPersistence;

    public ChatService(
            IntentRouter intentRouter,
            ModeRegistry modeRegistry,
            UserConstants userConstants,
            ModePlanExecutor modePlanExecutor,
            ChatRequestValidator chatRequestValidator,
            UserPromptPreparationService userPromptPreparationService,
            UserPromptCipherPersistenceService userPromptCipherPersistenceService,
            ChatStmHistoryAssembler chatStmHistoryAssembler,
            ConversationStmBuffer conversationStmBuffer,
            Jingu3Properties jingu3Properties,
            UserCorrectionMemoryPersistence userCorrectionMemoryPersistence) {
        this.intentRouter = intentRouter;
        this.modeRegistry = modeRegistry;
        this.userConstants = userConstants;
        this.modePlanExecutor = modePlanExecutor;
        this.chatRequestValidator = chatRequestValidator;
        this.userPromptPreparationService = userPromptPreparationService;
        this.userPromptCipherPersistenceService = userPromptCipherPersistenceService;
        this.chatStmHistoryAssembler = chatStmHistoryAssembler;
        this.conversationStmBuffer = conversationStmBuffer;
        this.jingu3Properties = jingu3Properties;
        this.userCorrectionMemoryPersistence = userCorrectionMemoryPersistence;
    }

    /**
     * 处理一轮聊天请求，返回完整 {@link ChatVo}（非流式）。
     *
     * @param request 含 message、可选 mode / modePlan / workflowId 等
     * @return 聚合后的回复与路由元数据
     */
    public ChatVo chat(ChatRequest request) {
        ChatInboundPlatformSupport.mergeIntoRequest(request, null);
        chatRequestValidator.validate(request);
        Instant serverTime = Instant.now();
        userPromptCipherPersistenceService.tryPersistRawUserMessage(
                userConstants.getId(), request.getConversationId(), request.getMessage());

        if (request.getModePlan() != null && !request.getModePlan().isEmpty()) {
            ChatVo vo = modePlanExecutor.execute(request, userConstants, serverTime);
            log.info("modePlan userId={} steps={} lastMode={} conv={}",
                    userConstants.getId(),
                    vo.getPlanSteps() != null ? vo.getPlanSteps().size() : 0,
                    vo.getActionMode(),
                    request.getConversationId());
            if (jingu3Properties.getChat().isStmEnabled()) {
                String conv = ChatStmHistoryAssembler.effectiveConversationId(request);
                String r = vo.getReply() == null ? "" : vo.getReply();
                conversationStmBuffer.recordTurn(conv, request.getMessage(), r);
            }
            userCorrectionMemoryPersistence.persistIfRequested(request);
            return vo;
        }

        String forLlm = userPromptPreparationService.prepare(request, userConstants.getId(), serverTime);
        RoutingDecision decision =
                RoutingFallbacks.askIfWorkflowWithoutWorkflowId(
                        intentRouter.resolve(request.getMessage(), request.getMode()), request.getWorkflowId());

        log.info("routing userId={} source={} mode={} note={} conv={}",
                userConstants.getId(),
                decision.getSource(),
                decision.getMode(),
                decision.getNote(),
                request.getConversationId());

        String conv = ChatStmHistoryAssembler.effectiveConversationId(request);
        List<String> history = chatStmHistoryAssembler.assemble(request);
        ExecutionContext ctx = new ExecutionContext(
                userConstants.getId(),
                userConstants.getUsername(),
                conv,
                forLlm,
                decision.getMode(),
                decision.getSource(),
                history,
                null,
                decision.getMode() == ActionMode.WORKFLOW ? request.getWorkflowId() : null);
        String reply = modeRegistry.get(decision.getMode()).execute(ctx);
        if (decision.getGuardUserNotice() != null && !decision.getGuardUserNotice().isBlank()) {
            reply = decision.getGuardUserNotice() + reply;
        }
        if (jingu3Properties.getChat().isStmEnabled()) {
            conversationStmBuffer.recordTurn(conv, request.getMessage(), reply == null ? "" : reply);
        }
        userCorrectionMemoryPersistence.persistIfRequested(request);

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
