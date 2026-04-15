package cn.lysoy.jingu3.service.chat;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.service.chat.UserPromptCipherPersistenceService;
import cn.lysoy.jingu3.component.ChatInboundPlatformSupport;
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
import cn.lysoy.jingu3.service.prompt.UserPromptPreparationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * <strong>同步对话编排入口</strong>（驾驭工程 + 上下文工程）：HTTP {@code POST /api/v1/chat} 的非流式路径。
 * <p><strong>管线顺序</strong>：平台头合并 → 请求校验 →（可选）用户句密文落库 →
 * 若带 {@code modePlan} 则走 {@link ModePlanExecutor}（多模式顺序执行）；否则经
 * {@link UserPromptPreparationService} 做记忆检索等<strong>送模前用户串改写</strong> →
 * {@link IntentRouter} 三源路由并经 {@link RoutingFallbacks} 处理 WORKFLOW 无 id 等回落 →
 * 构造 {@link ExecutionContext} → {@link ModeRegistry} 选中 {@link cn.lysoy.jingu3.engine.ActionModeHandler#execute}。</p>
 * <p>流式输出见 {@link ChatStreamService}；二者应保持路由与上下文构造语义一致。</p>
 */
@Slf4j
@Service
public class ChatService {

    /** 三源路由：显式 mode &gt; 规则 &gt; 模型分类 */
    private final IntentRouter intentRouter;
    /** 八大模式分派表 */
    private final ModeRegistry modeRegistry;
    /** 单用户种子（路线图 v0.8 前固定 001/user） */
    private final UserConstants userConstants;
    /** 指南 §11：客户端显式 modePlan 顺序编排 */
    private final ModePlanExecutor modePlanExecutor;
    /** 对话请求体合法性、模式与 workflowId 等守门 */
    private final ChatRequestValidator chatRequestValidator;

    /** 记忆注入 + UTC/平台行等，对送模 user 串的最终形态负责 */
    private final UserPromptPreparationService userPromptPreparationService;

    /** 可选：原始用户句 AES-GCM 落库（横切审计） */
    private final UserPromptCipherPersistenceService userPromptCipherPersistenceService;

    public ChatService(
            IntentRouter intentRouter,
            ModeRegistry modeRegistry,
            UserConstants userConstants,
            ModePlanExecutor modePlanExecutor,
            ChatRequestValidator chatRequestValidator,
            UserPromptPreparationService userPromptPreparationService,
            UserPromptCipherPersistenceService userPromptCipherPersistenceService) {
        this.intentRouter = intentRouter;
        this.modeRegistry = modeRegistry;
        this.userConstants = userConstants;
        this.modePlanExecutor = modePlanExecutor;
        this.chatRequestValidator = chatRequestValidator;
        this.userPromptPreparationService = userPromptPreparationService;
        this.userPromptCipherPersistenceService = userPromptCipherPersistenceService;
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

        // 指南 §11：显式 modePlan 优先于单次 mode 路由，由 ModePlanExecutor 顺序驱动多种 ActionMode
        if (request.getModePlan() != null && !request.getModePlan().isEmpty()) {
            ChatVo vo = modePlanExecutor.execute(request, userConstants, serverTime);
            log.info("modePlan userId={} steps={} lastMode={} conv={}",
                    userConstants.getId(),
                    vo.getPlanSteps() != null ? vo.getPlanSteps().size() : 0,
                    vo.getActionMode(),
                    request.getConversationId());
            return vo;
        }

        // 上下文工程：记忆检索片段 + 时间/平台行等拼入送模串（与原始 message 解耦）
        String forLlm = userPromptPreparationService.prepare(request, userConstants.getId(), serverTime);
        // 意图工程：三源路由后，对工作流无 id 等场景做安全回落
        RoutingDecision decision =
                RoutingFallbacks.askIfWorkflowWithoutWorkflowId(
                        intentRouter.resolve(request.getMessage(), request.getMode()), request.getWorkflowId());

        log.info("routing userId={} source={} mode={} note={} conv={}",
                userConstants.getId(),
                decision.getSource(),
                decision.getMode(),
                decision.getNote(),
                request.getConversationId());

        // 组装本步执行上下文：userMessage 字段承载「已增强」串，供 PromptAssembly 与各类 Handler 使用
        ExecutionContext ctx = new ExecutionContext(
                userConstants.getId(),
                userConstants.getUsername(),
                request.getConversationId() == null || request.getConversationId().isBlank()
                        ? ConversationConstants.DEFAULT_CONVERSATION_ID
                        : request.getConversationId(),
                forLlm,
                decision.getMode(),
                decision.getSource(),
                List.of(),
                null,
                decision.getMode() == ActionMode.WORKFLOW ? request.getWorkflowId() : null
        );
        // 八大模式之一：同步阻塞执行
        String reply = modeRegistry.get(decision.getMode()).execute(ctx);
        // 显式模式守门：在正文前拼接已格式化好的用户可见说明（如从 Plan/AgentTeam 降为 ASK）
        if (decision.getGuardUserNotice() != null && !decision.getGuardUserNotice().isBlank()) {
            reply = decision.getGuardUserNotice() + reply;
        }
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
