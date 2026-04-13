package cn.lysoy.jingu3.prompt;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.constant.PromptTemplates;
import cn.lysoy.jingu3.engine.ExecutionContext;
import org.springframework.stereotype.Component;

/**
 * 提示词拼装（ENG:提示词工程）：将 {@link PromptTemplates} 中的系统/角色文案与 {@link PromptFragments}
 * 中的换行、标签等拼成最终送入 LLM 的字符串；每条对话均在段首注入 {@link ModeRoutingPreamble}，
 * 标明路由来源、当前模式与各模式含义，便于模型善意提示用户切换模式。
 */
@Component
public class PromptAssembly {

    /**
     * Ask 系统提示原文（不含路由前缀；一般应使用 {@link #buildAskCombinedPrompt(ExecutionContext)}）。
     */
    public String askSystemPrompt() {
        return PromptTemplates.ASK_SYSTEM;
    }

    /**
     * ReAct 系统提示原文（不含路由前缀）。
     */
    public String reactSystemPrompt() {
        return PromptTemplates.REACT_SYSTEM;
    }

    private static String withModePreamble(ExecutionContext ctx, String body) {
        return ModeRoutingPreamble.build(ctx) + PromptFragments.PARAGRAPH_BREAK + body;
    }

    /**
     * Ask：路由说明 + 系统角色 + 用户段落。
     */
    public String buildAskCombinedPrompt(ExecutionContext ctx) {
        return withModePreamble(
                ctx,
                PromptTemplates.ASK_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + ctx.llmInput());
    }

    /**
     * ReAct：路由说明 + 系统角色 + 用户段落。
     */
    public String buildReactCombinedPrompt(ExecutionContext ctx) {
        return withModePreamble(
                ctx,
                PromptTemplates.REACT_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + ctx.llmInput());
    }

    public String buildPlanAndExecutePlanPrompt(ExecutionContext ctx) {
        return withModePreamble(
                ctx,
                PromptTemplates.PLAN_AND_EXECUTE_PLAN_ONLY
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + ctx.llmInput());
    }

    public String buildPlanAndExecuteExecutePrompt(
            ExecutionContext ctx, String planText, String originalUserMessage) {
        return withModePreamble(
                ctx,
                PromptTemplates.PLAN_AND_EXECUTE_EXECUTE_HEADER
                        + PromptFragments.PARAGRAPH_BREAK
                        + "计划：\n"
                        + planText
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + originalUserMessage);
    }

    public String buildWorkflowStep1Prompt(ExecutionContext ctx, String llmInput) {
        return withModePreamble(
                ctx,
                PromptTemplates.WORKFLOW_STEP1
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + llmInput);
    }

    public String buildWorkflowStep2Prompt(ExecutionContext ctx, String summary, String originalUserMessage) {
        return withModePreamble(
                ctx,
                PromptTemplates.WORKFLOW_STEP2_HEADER
                        + PromptFragments.PARAGRAPH_BREAK
                        + summary
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + originalUserMessage);
    }

    public String buildAgentTeamLeadPrompt(ExecutionContext ctx) {
        return withModePreamble(
                ctx,
                PromptTemplates.AGENT_TEAM_LEAD
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + ctx.llmInput());
    }

    public String buildAgentTeamSubPrompt(ExecutionContext ctx, String subtask) {
        return withModePreamble(ctx, PromptTemplates.AGENT_TEAM_SUB_PREFIX + subtask);
    }

    /**
     * ReAct 多步循环中单步提示（指南 §4：思考→行动→观察）。
     */
    public String buildReactLoopStepPrompt(
            ExecutionContext ctx, String userMessage, String priorTrace, int stepIndex, int maxSteps) {
        return withModePreamble(
                ctx,
                PromptTemplates.REACT_LOOP_INSTRUCTION
                        + PromptFragments.PARAGRAPH_BREAK
                        + "当前第 "
                        + stepIndex
                        + " 步（最多 "
                        + maxSteps
                        + " 步）。"
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + userMessage
                        + PromptFragments.PARAGRAPH_BREAK
                        + "已有过程：\n"
                        + (priorTrace == null || priorTrace.isBlank() ? "（无）" : priorTrace));
    }

    public String buildSubtaskExecutePrompt(
            ExecutionContext ctx, String subtask, String originalUserMessage, int stepNumber) {
        return withModePreamble(
                ctx,
                PromptTemplates.SUBTASK_EXECUTE_HEADER
                        + "（第 "
                        + stepNumber
                        + " 步）\n"
                        + subtask
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + originalUserMessage);
    }

    public String buildReplannerPrompt(
            ExecutionContext ctx, String failedPlan, String errorHint, String originalUserMessage) {
        return withModePreamble(
                ctx,
                PromptTemplates.REPLANNER_HEADER
                        + PromptFragments.PARAGRAPH_BREAK
                        + "原计划：\n"
                        + failedPlan
                        + PromptFragments.PARAGRAPH_BREAK
                        + "错误："
                        + errorHint
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + originalUserMessage);
    }

    public String buildWorkflowNodePrompt(
            ExecutionContext ctx, String nodeInstruction, String accumulatedContext, String originalUserMessage) {
        return withModePreamble(
                ctx,
                PromptTemplates.WORKFLOW_NODE_HEADER
                        + nodeInstruction
                        + PromptFragments.PARAGRAPH_BREAK
                        + "当前上下文与中间结果：\n"
                        + accumulatedContext
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + originalUserMessage);
    }
}
