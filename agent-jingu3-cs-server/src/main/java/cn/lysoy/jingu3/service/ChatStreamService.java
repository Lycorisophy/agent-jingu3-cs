package cn.lysoy.jingu3.service;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.dto.ChatRequest;
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

import java.util.List;

/**
 * 流式对话编排：与 {@link ChatService} 共享「校验 + 路由 + 上下文构造」，但输出为
 * {@link cn.lysoy.jingu3.stream.StreamEvent} 序列而非单次 JSON。
 * <ul>
 *   <li>Ask：Ollama 流式 API → {@link StreamEventSink#token}；</li>
 *   <li>多步模式：每轮 LLM 以 {@link StreamEventSink#stepBegin}/{@link StreamEventSink#block}/{@link StreamEventSink#stepEnd} 暴露；</li>
 *   <li>modePlan：与同步版一致逐步 {@link cn.lysoy.jingu3.engine.ActionModeHandler#execute}，仅块式推送，避免中途 DONE。</li>
 * </ul>
 * 线程：实际推理在 {@code chatStreamExecutor} 中执行，避免阻塞 Tomcat 工作线程。
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
            ObjectMapper objectMapper) {
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
     * 流式管线核心：单入口，供测试或扩展直接调用。
     */
    void runStream(ChatRequest request, StreamEventSink sink) {
        try {
            chatRequestValidator.validate(request);
            if (request.getModePlan() != null && !request.getModePlan().isEmpty()) {
                streamModePlan(request, sink);
                return;
            }
            RoutingDecision decision =
                    RoutingFallbacks.askIfWorkflowWithoutWorkflowId(
                            intentRouter.resolve(request.getMessage(), request.getMode()), request.getWorkflowId());

            sink.meta(
                    decision.getMode().name(),
                    decision.getSource().name(),
                    userConstants.getId(),
                    userConstants.getUsername());

            ExecutionContext ctx = buildContext(request, decision);
            streamByMode(decision.getMode(), ctx, sink);
        } catch (Exception e) {
            log.warn("stream pipeline failed: {}", e.toString());
            sink.error(e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    /**
     * 与 {@link ModePlanExecutor} 对齐的顺序执行；每步用阻塞 execute，以 BLOCK 推送（不做 Ask 的 TOKEN 流，避免步间提前 DONE）。
     */
    private void streamModePlan(ChatRequest request, StreamEventSink sink) {
        List<String> raw = request.getModePlan();
        if (raw.size() > ModePlanExecutor.MAX_STEPS) {
            raw = raw.subList(0, ModePlanExecutor.MAX_STEPS);
        }
        String conv = request.getConversationId() == null || request.getConversationId().isBlank()
                ? "default"
                : request.getConversationId();
        sink.meta(
                "MODE_PLAN",
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
                    request.getMessage(),
                    mode,
                    RoutingSource.CLIENT_EXPLICIT,
                    List.of(),
                    chainPayload,
                    workflowId);
            String reply = modeRegistry.get(mode).execute(ctx);
            sink.block(reply == null ? "" : reply);
            sink.stepEnd(stepNum);
            // 与 ModePlanExecutor 相同：把「用户消息 + 上步答复」交给下一步作为 taskPayload
            chainPayload = request.getMessage() + PromptFragments.PLAN_CHAIN_SEPARATOR + reply;
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

    private ExecutionContext buildContext(ChatRequest request, RoutingDecision decision) {
        String conv = request.getConversationId() == null || request.getConversationId().isBlank()
                ? "default"
                : request.getConversationId();
        return new ExecutionContext(
                userConstants.getId(),
                userConstants.getUsername(),
                conv,
                request.getMessage(),
                decision.getMode(),
                decision.getSource(),
                List.of(),
                null,
                decision.getMode() == ActionMode.WORKFLOW ? request.getWorkflowId() : null);
    }

    /**
     * 按枚举分发到各 handler 的 {@code stream} 或回退为 execute+BLOCK（无流式实现的模式）。
     */
    private void streamByMode(ActionMode mode, ExecutionContext ctx, StreamEventSink sink) {
        switch (mode) {
            case ASK -> askModeHandler.stream(ctx, sink);
            case REACT -> reActModeHandler.stream(ctx, sink);
            case PLAN_AND_EXECUTE -> planAndExecuteModeHandler.stream(ctx, sink);
            case WORKFLOW -> workflowModeHandler.stream(ctx, sink);
            case AGENT_TEAM -> agentTeamModeHandler.stream(ctx, sink);
            case CRON -> {
                sink.stepBegin(1, "cron");
                sink.block(cronModeHandler.execute(ctx));
                sink.stepEnd(1);
                sink.done();
            }
            case STATE_TRACKING -> {
                sink.stepBegin(1, "state_tracking");
                sink.block(stateTrackingModeHandler.execute(ctx));
                sink.stepEnd(1);
                sink.done();
            }
            case HUMAN_IN_LOOP -> {
                sink.stepBegin(1, "human_in_loop");
                sink.block(humanInLoopModeHandler.execute(ctx));
                sink.stepEnd(1);
                sink.done();
            }
        }
    }
}
