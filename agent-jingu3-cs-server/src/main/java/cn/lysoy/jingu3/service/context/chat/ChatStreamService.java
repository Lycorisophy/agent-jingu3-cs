package cn.lysoy.jingu3.service.context.chat;

import cn.lysoy.jingu3.common.constant.ChatApiConstants;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.component.ChatInboundPlatformSupport;
import cn.lysoy.jingu3.component.ChatRequestValidator;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.guard.ActionMode;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.guard.ModeRegistry;
import cn.lysoy.jingu3.service.mode.handler.AgentTeamModeHandler;
import cn.lysoy.jingu3.service.mode.handler.AskModeHandler;
import cn.lysoy.jingu3.service.mode.handler.CronModeHandler;
import cn.lysoy.jingu3.service.mode.handler.HumanInLoopModeHandler;
import cn.lysoy.jingu3.service.mode.handler.PlanAndExecuteModeHandler;
import cn.lysoy.jingu3.service.mode.handler.ReActModeHandler;
import cn.lysoy.jingu3.service.mode.handler.StateTrackingModeHandler;
import cn.lysoy.jingu3.service.mode.handler.WorkflowModeHandler;
import cn.lysoy.jingu3.service.mode.orchestration.ModePlanExecutor;
import cn.lysoy.jingu3.service.guard.routing.IntentRouter;
import cn.lysoy.jingu3.service.guard.routing.RoutingDecision;
import cn.lysoy.jingu3.service.guard.routing.RoutingFallbacks;
import cn.lysoy.jingu3.service.guard.routing.RoutingSource;
import cn.lysoy.jingu3.service.context.prepare.UserPromptPreparationService;
import cn.lysoy.jingu3.service.context.stream.SseStreamEventSink;
import cn.lysoy.jingu3.service.context.stream.StmRecordingStreamEventSink;
import cn.lysoy.jingu3.service.context.stream.StreamErrorMessages;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import cn.lysoy.jingu3.service.context.stream.WebSocketStreamEventSink;
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
 * <strong>流式对话编排入口</strong>（驾驭工程 + 上下文工程）：SSE / WebSocket 将 {@link cn.lysoy.jingu3.service.context.stream.StreamEvent}
 * 序列推给客户端，与 {@link ChatService} 在「校验、记忆增强、三源路由、modePlan、ExecutionContext」上语义对齐。
 */
@Slf4j
@Service
public class ChatStreamService {

    private final ChatRequestValidator chatRequestValidator;
    private final IntentRouter intentRouter;
    private final ModeRegistry modeRegistry;
    private final UserConstants userConstants;
    private final AskModeHandler askModeHandler;
    private final ReActModeHandler reActModeHandler;
    private final PlanAndExecuteModeHandler planAndExecuteModeHandler;
    private final WorkflowModeHandler workflowModeHandler;
    private final AgentTeamModeHandler agentTeamModeHandler;
    private final CronModeHandler cronModeHandler;
    private final StateTrackingModeHandler stateTrackingModeHandler;
    private final HumanInLoopModeHandler humanInLoopModeHandler;
    private final TaskExecutor chatStreamExecutor;
    private final ObjectMapper objectMapper;
    private final UserPromptPreparationService userPromptPreparationService;
    private final UserPromptCipherPersistenceService userPromptCipherPersistenceService;
    private final ChatStmHistoryAssembler chatStmHistoryAssembler;
    private final ConversationStmBuffer conversationStmBuffer;
    private final Jingu3Properties jingu3Properties;
    private final UserCorrectionMemoryPersistence userCorrectionMemoryPersistence;

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
            UserPromptCipherPersistenceService userPromptCipherPersistenceService,
            ChatStmHistoryAssembler chatStmHistoryAssembler,
            ConversationStmBuffer conversationStmBuffer,
            Jingu3Properties jingu3Properties,
            UserCorrectionMemoryPersistence userCorrectionMemoryPersistence) {
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
        this.chatStmHistoryAssembler = chatStmHistoryAssembler;
        this.conversationStmBuffer = conversationStmBuffer;
        this.jingu3Properties = jingu3Properties;
        this.userCorrectionMemoryPersistence = userCorrectionMemoryPersistence;
    }

    public void startSseStream(ChatRequest request, SseEmitter emitter) {
        SseStreamEventSink sink = new SseStreamEventSink(emitter, objectMapper);
        chatStreamExecutor.execute(() -> runStream(request, sink));
    }

    public void startWebSocketStream(ChatRequest request, WebSocketSession session) {
        WebSocketStreamEventSink sink = new WebSocketStreamEventSink(session, objectMapper);
        chatStreamExecutor.execute(() -> runStream(request, sink));
    }

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
            String forLlm = userPromptPreparationService.prepare(request, userConstants.getId(), serverTime);
            RoutingDecision decision =
                    RoutingFallbacks.askIfWorkflowWithoutWorkflowId(
                            intentRouter.resolve(request.getMessage(), request.getMode()), request.getWorkflowId());

            sink.meta(
                    decision.getMode().name(),
                    decision.getSource().name(),
                    userConstants.getId(),
                    userConstants.getUsername());

            if (decision.getGuardUserNotice() != null && !decision.getGuardUserNotice().isBlank()) {
                sink.block(decision.getGuardUserNotice());
            }

            String conv = ChatStmHistoryAssembler.effectiveConversationId(request);
            List<String> history = chatStmHistoryAssembler.assemble(request);
            ExecutionContext ctx = buildContext(request, decision, forLlm, history);

            StreamEventSink effectiveSink =
                    jingu3Properties.getChat().isStmEnabled()
                            ? new StmRecordingStreamEventSink(
                                    sink, conversationStmBuffer, true, conv, request.getMessage())
                            : sink;
            streamByMode(decision.getMode(), ctx, effectiveSink);
            userCorrectionMemoryPersistence.persistIfRequested(request);
        } catch (Exception e) {
            log.warn("stream pipeline failed: {}", e.toString());
            sink.error(StreamErrorMessages.fromThrowable(e));
        }
    }

    private void streamModePlan(ChatRequest request, StreamEventSink sink, Instant serverTime) {
        List<String> raw = request.getModePlan();
        if (raw.size() > ModePlanExecutor.MAX_STEPS) {
            raw = raw.subList(0, ModePlanExecutor.MAX_STEPS);
        }
        String effectiveMessage = userPromptPreparationService.prepare(request, userConstants.getId(), serverTime);
        String conv = ChatStmHistoryAssembler.effectiveConversationId(request);
        List<String> history = chatStmHistoryAssembler.assemble(request);
        sink.meta(
                ChatApiConstants.STREAM_META_ACTION_MODE_MODE_PLAN,
                RoutingSource.CLIENT_EXPLICIT.name(),
                userConstants.getId(),
                userConstants.getUsername());
        String chainPayload = null;
        int stepNum = 0;
        StringBuilder combined = new StringBuilder();
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
                    history,
                    chainPayload,
                    workflowId);
            String reply = modeRegistry.get(mode).execute(ctx);
            String block = reply == null ? "" : reply;
            sink.block(block);
            combined.append(block).append('\n');
            sink.stepEnd(stepNum);
            chainPayload = effectiveMessage + PromptFragments.PLAN_CHAIN_SEPARATOR + reply;
        }
        if (jingu3Properties.getChat().isStmEnabled()) {
            conversationStmBuffer.recordTurn(conv, request.getMessage(), combined.toString().trim());
        }
        userCorrectionMemoryPersistence.persistIfRequested(request);
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

    private ExecutionContext buildContext(
            ChatRequest request, RoutingDecision decision, String messageForLlm, List<String> history) {
        String conv = ChatStmHistoryAssembler.effectiveConversationId(request);
        return new ExecutionContext(
                userConstants.getId(),
                userConstants.getUsername(),
                conv,
                messageForLlm,
                decision.getMode(),
                decision.getSource(),
                history,
                null,
                decision.getMode() == ActionMode.WORKFLOW ? request.getWorkflowId() : null);
    }

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
