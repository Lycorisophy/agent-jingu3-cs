package cn.lysoy.jingu3.service;

import cn.lysoy.jingu3.common.constant.ChatApiConstants;
import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.chat.UserPromptCipherPersistenceService;
import cn.lysoy.jingu3.component.ChatInboundPlatformSupport;
import cn.lysoy.jingu3.component.ChatRequestValidator;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.ModeRegistry;
import cn.lysoy.jingu3.engine.mode.AgentTeamModeHandler;
import cn.lysoy.jingu3.engine.mode.AskModeHandler;
import cn.lysoy.jingu3.engine.mode.CronModeHandler;
import cn.lysoy.jingu3.engine.mode.HumanInLoopModeHandler;
import cn.lysoy.jingu3.engine.mode.PlanAndExecuteModeHandler;
import cn.lysoy.jingu3.engine.mode.ReActModeHandler;
import cn.lysoy.jingu3.engine.mode.StateTrackingModeHandler;
import cn.lysoy.jingu3.engine.mode.WorkflowModeHandler;
import cn.lysoy.jingu3.engine.orchestration.ModePlanExecutor;
import cn.lysoy.jingu3.engine.routing.IntentRouter;
import cn.lysoy.jingu3.engine.routing.RoutingDecision;
import cn.lysoy.jingu3.engine.routing.RoutingFallbacks;
import cn.lysoy.jingu3.engine.routing.RoutingSource;
import cn.lysoy.jingu3.prompt.UserPromptPreparationService;
import cn.lysoy.jingu3.stream.SseStreamEventSink;
import cn.lysoy.jingu3.stream.StreamEventSink;
import cn.lysoy.jingu3.stream.WebSocketStreamEventSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.List;

/**
 * <strong>流式对话编排入口</strong>（驾驭工程 + 上下文工程）：SSE / WebSocket 将 {@link cn.lysoy.jingu3.stream.StreamEvent}
 * 序列推给客户端，与 {@link ChatService} 在「校验、记忆增强、三源路由、modePlan、ExecutionContext」上语义对齐。
 * <ul>
 *   <li><strong>Ask</strong>：流式模型 API → {@link StreamEventSink#token}；工具路径上可先发 {@link StreamEventSink#toolResult}；</li>
 *   <li><strong>ReAct / Plan / Workflow / Agent Team</strong> 等：用 {@link StreamEventSink#stepBegin}、{@link StreamEventSink#block}、
 *   {@link StreamEventSink#stepEnd} 暴露子步，便于 UI 展示「驾驭」过程；</li>
 *   <li><strong>modePlan</strong>：与 {@link ModePlanExecutor} 同步顺序执行，每步 {@code execute}+BLOCK，避免步间误发 DONE。</li>
 * </ul>
 * <p><strong>线程模型</strong>：实际推理在 {@code chatStreamExecutor} 中执行，避免长时间占用 Tomcat 工作线程导致连接超时。</p>
 */
@Slf4j
@Service
public class ChatStreamService {

    private final ChatRequestValidator chatRequestValidator;
    private final IntentRouter intentRouter;
    /** 流式路径仍通过 Registry 取 Handler，但部分模式直接注入具体类以调用 {@code stream}（避免重复 switch 扩散） */
    private final ModeRegistry modeRegistry;
    private final UserConstants userConstants;
    /** 指南 §3 */
    private final AskModeHandler askModeHandler;
    /** 指南 §4 */
    private final ReActModeHandler reActModeHandler;
    /** 指南 §5 */
    private final PlanAndExecuteModeHandler planAndExecuteModeHandler;
    /** 指南 §6：JSON 顺序工作流 */
    private final WorkflowModeHandler workflowModeHandler;
    /** 指南 §7：Leader + 专员 + 合成 */
    private final AgentTeamModeHandler agentTeamModeHandler;
    /** 指南 §8：定时意图说明（Stub/MVP） */
    private final CronModeHandler cronModeHandler;
    /** 指南 §9：状态追踪占位 */
    private final StateTrackingModeHandler stateTrackingModeHandler;
    /** 指南 §10：人在环说明 */
    private final HumanInLoopModeHandler humanInLoopModeHandler;
    /** 异步执行流式管线的线程池（见配置 Bean 名 {@code chatStreamExecutor}） */
    private final TaskExecutor chatStreamExecutor;
    /** 将 StreamEvent 序列化为与 SSE data 一致的 JSON 行 */
    private final ObjectMapper objectMapper;

    /** 同 {@link ChatService}：记忆注入 + 送模前格式化 */
    private final UserPromptPreparationService userPromptPreparationService;

    private final UserPromptCipherPersistenceService userPromptCipherPersistenceService;

    public ChatStreamService(
            ChatRequestValidator chatRequestValidator,
            IntentRouter intentRouter,
            ModeRegistry modeRegistry,
            UserConstants userConstants,
            AskModeHandler askModeHandler,
            ReActModeHandler reActModeHandler,
            PlanAndExecuteModeHandler planAndExecuteModeHandler,
            WorkflowModeHandler workflowModeHandler,
            AgentTeamModeHandler agentTeamModeHandler,
            CronModeHandler cronModeHandler,
            StateTrackingModeHandler stateTrackingModeHandler,
            HumanInLoopModeHandler humanInLoopModeHandler,
            @Qualifier("chatStreamExecutor") TaskExecutor chatStreamExecutor,
            ObjectMapper objectMapper,
            UserPromptPreparationService userPromptPreparationService,
            UserPromptCipherPersistenceService userPromptCipherPersistenceService) {
        this.chatRequestValidator = chatRequestValidator;
        this.intentRouter = intentRouter;
        this.modeRegistry = modeRegistry;
        this.userConstants = userConstants;
        this.askModeHandler = askModeHandler;
        this.reActModeHandler = reActModeHandler;
        this.planAndExecuteModeHandler = planAndExecuteModeHandler;
        this.workflowModeHandler = workflowModeHandler;
        this.agentTeamModeHandler = agentTeamModeHandler;
        this.cronModeHandler = cronModeHandler;
        this.stateTrackingModeHandler = stateTrackingModeHandler;
        this.humanInLoopModeHandler = humanInLoopModeHandler;
        this.chatStreamExecutor = chatStreamExecutor;
        this.objectMapper = objectMapper;
        this.userPromptPreparationService = userPromptPreparationService;
        this.userPromptCipherPersistenceService = userPromptCipherPersistenceService;
    }

    /**
     * 提交异步任务：将 {@link SseStreamEventSink} 绑到 {@code emitter}，在后台线程执行 {@link #runStream}。
     */
    public void startSseStream(ChatRequest request, SseEmitter emitter) {
        SseStreamEventSink sink = new SseStreamEventSink(emitter, objectMapper);
        chatStreamExecutor.execute(() -> runStream(request, sink));
    }

    /**
     * 提交异步任务：事件写入 WebSocket 文本帧（JSON 与 SSE data 一致）。
     */
    public void startWebSocketStream(ChatRequest request, WebSocketSession session) {
        WebSocketStreamEventSink sink = new WebSocketStreamEventSink(session, objectMapper);
        chatStreamExecutor.execute(() -> runStream(request, sink));
    }

    /**
     * 流式管线核心：单入口，供 SSE/WebSocket 启动方法或单测直接调用。
     */
    void runStream(ChatRequest request, StreamEventSink sink) {
        try {
            ChatInboundPlatformSupport.mergeIntoRequest(request, null);
            chatRequestValidator.validate(request);
            Instant serverTime = Instant.now();
            userPromptCipherPersistenceService.tryPersistRawUserMessage(
                    userConstants.getId(), request.getConversationId(), request.getMessage());
            if (request.getModePlan() != null && !request.getModePlan().isEmpty()) {
                streamModePlan(request, sink, serverTime);
                return;
            }
            // 与 ChatService 一致：先上下文工程再意图路由
            String forLlm = userPromptPreparationService.prepare(request, userConstants.getId(), serverTime);
            RoutingDecision decision =
                    RoutingFallbacks.askIfWorkflowWithoutWorkflowId(
                            intentRouter.resolve(request.getMessage(), request.getMode()), request.getWorkflowId());

            // 首帧 meta：客户端可展示当前模式与路由来源（对齐客户端 UI 规范）
            sink.meta(
                    decision.getMode().name(),
                    decision.getSource().name(),
                    userConstants.getId(),
                    userConstants.getUsername());

            if (decision.getGuardUserNotice() != null && !decision.getGuardUserNotice().isBlank()) {
                sink.block(decision.getGuardUserNotice());
            }

            ExecutionContext ctx = buildContext(request, decision, forLlm);
            // 按枚举分发到各模式 stream；各实现内部负责 TOKEN/BLOCK/DONE 约定
            streamByMode(decision.getMode(), ctx, sink);
        } catch (Exception e) {
            log.warn("stream pipeline failed: {}", e.toString());
            sink.error(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    /**
     * modePlan 流式：与 {@link ModePlanExecutor} 对齐的顺序执行；每步阻塞 {@code execute} 后以整块 BLOCK 推送，
     * 避免单步内部 TOKEN 流导致客户端提前收到 DONE（与同步多步语义一致）。
     */
    private void streamModePlan(ChatRequest request, StreamEventSink sink, Instant serverTime) {
        List<String> raw = request.getModePlan();
        if (raw.size() > ModePlanExecutor.MAX_STEPS) {
            raw = raw.subList(0, ModePlanExecutor.MAX_STEPS);
        }
        String effectiveMessage = userPromptPreparationService.prepare(request, userConstants.getId(), serverTime);
        String conv = request.getConversationId() == null || request.getConversationId().isBlank()
                ? ConversationConstants.DEFAULT_CONVERSATION_ID
                : request.getConversationId();
        sink.meta(
                ChatApiConstants.STREAM_META_ACTION_MODE_MODE_PLAN,
                RoutingSource.CLIENT_EXPLICIT.name(),
                userConstants.getId(),
                userConstants.getUsername());
        String chainPayload = null;
        int stepNum = 0;
        for (String token : raw) {
            ActionMode mode =
                    RoutingFallbacks.modePlanStepOrAskIfWorkflowWithoutId(parsePlanToken(token), request.getWorkflowId());
            stepNum++;
            sink.stepBegin(stepNum, mode.name());
            String workflowId = mode == ActionMode.WORKFLOW ? request.getWorkflowId() : null;
            ExecutionContext ctx = new ExecutionContext(
                    userConstants.getId(),
                    userConstants.getUsername(),
                    conv,
                    effectiveMessage,
                    mode,
                    RoutingSource.CLIENT_EXPLICIT,
                    List.of(),
                    chainPayload,
                    workflowId);
            String reply = modeRegistry.get(mode).execute(ctx);
            sink.block(reply == null ? "" : reply);
            sink.stepEnd(stepNum);
            // 与 ModePlanExecutor 相同：把「用户消息 + 上步答复」交给下一步作为 taskPayload
            chainPayload = effectiveMessage + PromptFragments.PLAN_CHAIN_SEPARATOR + reply;
        }
        sink.done();
    }

    private static ActionMode parsePlanToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return ActionMode.REACT;
        }
        try {
            return ActionMode.fromFlexibleName(raw);
        } catch (IllegalArgumentException ex) {
            return ActionMode.REACT;
        }
    }

    private ExecutionContext buildContext(ChatRequest request, RoutingDecision decision, String messageForLlm) {
        String conv = request.getConversationId() == null || request.getConversationId().isBlank()
                ? ConversationConstants.DEFAULT_CONVERSATION_ID
                : request.getConversationId();
        return new ExecutionContext(
                userConstants.getId(),
                userConstants.getUsername(),
                conv,
                messageForLlm,
                decision.getMode(),
                decision.getSource(),
                List.of(),
                null,
                decision.getMode() == ActionMode.WORKFLOW ? request.getWorkflowId() : null);
    }

    /**
     * 按 {@link ActionMode} 分发到对应 Handler 的 {@code stream}；各模式自行约定 step/token 事件形态。
     */
    private void streamByMode(ActionMode mode, ExecutionContext ctx, StreamEventSink sink) {
        switch (mode) {
            case ASK -> askModeHandler.stream(ctx, sink);
            case REACT -> reActModeHandler.stream(ctx, sink);
            case PLAN_AND_EXECUTE -> planAndExecuteModeHandler.stream(ctx, sink);
            case WORKFLOW -> workflowModeHandler.stream(ctx, sink);
            case AGENT_TEAM -> agentTeamModeHandler.stream(ctx, sink);
            case CRON -> cronModeHandler.stream(ctx, sink);
            case STATE_TRACKING -> stateTrackingModeHandler.stream(ctx, sink);
            case HUMAN_IN_LOOP -> humanInLoopModeHandler.stream(ctx, sink);
        }
    }
}
