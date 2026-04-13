package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.plan.PlanTextParser;
import cn.lysoy.jingu3.engine.support.ToolStepService;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.stream.StreamEventSink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 指南 §5 Plan-and-Execute：先 Planner 产出编号计划，再 Executor 逐步执行子任务；失败时可 Replanner 修订计划。
 * 业界对比：相对 ReAct 减少「每步都问模型」的开销，适合步骤结构较清晰的任务。
 * <p>
 * 本实现：计划与子任务均为阻塞 {@code generate}；子任务列表由 {@link PlanTextParser} 从自然语言中抽取编号行；
 * 重规划最多一次（{@code replan-enabled}），与「最小 Replanner」一致。
 * </p>
 */
@Slf4j
@Component
public class PlanAndExecuteModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;
    private final ToolStepService toolStepService;
    private final int maxSubtasks;
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
                String newPlan = chat.generate(
                        prompts.buildReplannerPrompt(ctx, plan, ex.getMessage(), originalUserMessage));
                sink.block(newPlan == null ? "" : newPlan);
                sink.stepEnd(stepIdx);
                // 仅允许一次重规划：afterReplan=true 时子任务失败将直接 error
                streamWithPlan(ctx, newPlan, originalUserMessage, true, sink, stepIdx + 1);
                return;
            }
            sink.error(ex.getMessage() != null ? ex.getMessage() : ex.toString());
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
                String newPlan = chat.generate(
                        prompts.buildReplannerPrompt(ctx, plan, ex.getMessage(), originalUserMessage));
                return executeWithPlan(ctx, newPlan, originalUserMessage, true);
            }
            throw ex;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【计划】\n").append(plan).append("\n\n【分步结果】\n");
        for (int i = 0; i < stepOutputs.size(); i++) {
            sb.append("\n---\n步骤 ").append(i + 1).append(":\n").append(stepOutputs.get(i));
        }
        return sb.toString();
    }
}
