package cn.lysoy.jingu3.service.mode.handler;

import cn.lysoy.jingu3.service.mode.ActionModeHandler;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.mode.plan.PlanTextParser;
import cn.lysoy.jingu3.service.mode.support.ToolStepService;
import cn.lysoy.jingu3.service.prompt.PromptAssembly;
import cn.lysoy.jingu3.service.context.stream.StreamErrorMessages;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <strong>指南 §5 Plan-and-Execute</strong>（八大行动模式之一，驾驭工程）：<strong>Planner</strong> 先产出自然语言计划，
 * <strong>Executor</strong> 再按子任务顺序执行；相对 ReAct 减少「每步都问模型是否结束」的开销，适合步骤边界较清晰的任务。
 * <p>
 * 本实现特点：计划与子任务均为阻塞 {@code generate}；子任务列表由 {@link PlanTextParser} 从计划文本中抽取编号行（非 JSON Schema，
 * 属 MVP 取舍）；子任务内工具路由复用 {@link ToolStepService}（与 Workflow TOOL 节点同套路）；失败时可选<strong>单次</strong>
 * Replanner（{@code jingu3.engine.plan.replan-enabled}），第二次失败则向上抛或流式 error。
 * </p>
 */
@Slf4j
@Component
public class PlanAndExecuteModeHandler implements ActionModeHandler {

    /** Planner / Executor / Replanner 共用的主推理模型 */
    private final ChatLanguageModel chat;
    /** 各阶段送模串拼装 */
    private final PromptAssembly prompts;
    /** 子任务内 Ask 式工具路由 + 汇总 */
    private final ToolStepService toolStepService;
    /** 防止计划过长耗尽 token/时间 */
    private final int maxSubtasks;
    /** 是否允许在子任务失败后自动重规划一次 */
    private final boolean replanEnabled;

    public PlanAndExecuteModeHandler(
            ChatLanguageModel chat,
            PromptAssembly prompts,
            ToolStepService toolStepService,
            @Value("${jingu3.engine.plan.max-subtasks:8}") int maxSubtasks,
            @Value("${jingu3.engine.plan.replan-enabled:true}") boolean replanEnabled) {
        this.chat = chat;
        this.prompts = prompts;
        this.toolStepService = toolStepService;
        this.maxSubtasks = Math.max(1, maxSubtasks);
        this.replanEnabled = replanEnabled;
    }

    @Override
    public String execute(ExecutionContext context) {
        // 阶段 1：仅生成计划文本（不执行工具）
        String plan = chat.generate(prompts.buildPlanAndExecutePlanPrompt(context));
        return executeWithPlan(context, plan, context.getUserMessage(), false);
    }

    /**
     * 流式：第 1 步为计划全文；后续每子任务一步；若触发重规划则插入 {@code replan} 步再递归执行新计划。
     */
    public void stream(ExecutionContext context, StreamEventSink sink) {
        sink.stepBegin(1, "plan");
        String plan = chat.generate(prompts.buildPlanAndExecutePlanPrompt(context));
        sink.block(plan == null ? "" : plan);
        sink.stepEnd(1);
        streamWithPlan(context, plan, context.getUserMessage(), false, sink, 2);
    }

    /**
     * 从某份计划文本开始执行子任务流式推送；{@code nextStepIndex} 为下一步全局步号（与同步版逻辑对齐）。
     */
    private void streamWithPlan(
            ExecutionContext ctx,
            String plan,
            String originalUserMessage,
            boolean afterReplan,
            StreamEventSink sink,
            int nextStepIndex) {
        List<String> subtasks = PlanTextParser.parseSubtasks(plan);
        if (subtasks.size() > maxSubtasks) {
            subtasks = subtasks.subList(0, maxSubtasks);
        }
        int stepIdx = nextStepIndex;
        try {
            // 阶段 2：逐步执行子任务；sink 非空时由 ToolStepService 在工具路径上发 TOOL_RESULT
            for (int i = 0; i < subtasks.size(); i++) {
                sink.stepBegin(stepIdx, "subtask_" + (i + 1));
                String r = toolStepService.runPlanSubtask(
                        ctx, subtasks.get(i), originalUserMessage, i + 1, sink);
                sink.block(r == null ? "" : r);
                sink.stepEnd(stepIdx);
                stepIdx++;
            }
        } catch (RuntimeException ex) {
            log.warn("plan step failed: {}", ex.toString());
            if (replanEnabled && !afterReplan) {
                sink.stepBegin(stepIdx, "replan");
                // Replanner：把失败计划与错误摘要喂给模型，生成新计划文本后再走同一执行管线
                String newPlan = chat.generate(
                        prompts.buildReplannerPrompt(ctx, plan, ex.getMessage(), originalUserMessage));
                sink.block(newPlan == null ? "" : newPlan);
                sink.stepEnd(stepIdx);
                // 仅允许一次重规划：afterReplan=true 时子任务失败将直接 error
                streamWithPlan(ctx, newPlan, originalUserMessage, true, sink, stepIdx + 1);
                return;
            }
            sink.error(StreamErrorMessages.fromThrowable(ex));
            return;
        }
        sink.done();
    }

    private String executeWithPlan(ExecutionContext ctx, String plan, String originalUserMessage, boolean afterReplan) {
        List<String> subtasks = PlanTextParser.parseSubtasks(plan);
        if (subtasks.size() > maxSubtasks) {
            subtasks = subtasks.subList(0, maxSubtasks);
        }
        List<String> stepOutputs = new ArrayList<>();
        try {
            for (int i = 0; i < subtasks.size(); i++) {
                String r = toolStepService.runPlanSubtask(
                        ctx, subtasks.get(i), originalUserMessage, i + 1, null);
                stepOutputs.add(r);
            }
        } catch (RuntimeException ex) {
            log.warn("plan step failed: {}", ex.toString());
            if (replanEnabled && !afterReplan) {
                // 同步路径：重规划后直接递归；afterReplan 为 true 时不再 replan，将异常交给全局异常处理
                String newPlan = chat.generate(
                        prompts.buildReplannerPrompt(ctx, plan, ex.getMessage(), originalUserMessage));
                return executeWithPlan(ctx, newPlan, originalUserMessage, true);
            }
            throw ex;
        }

        // 汇总：便于人类阅读；客户端若以 JSON 消费可只取最后一步或自行解析
        StringBuilder sb = new StringBuilder();
        sb.append("【计划】\n").append(plan).append("\n\n【分步结果】\n");
        for (int i = 0; i < stepOutputs.size(); i++) {
            sb.append("\n---\n步骤 ").append(i + 1).append(":\n").append(stepOutputs.get(i));
        }
        return sb.toString();
    }
}
