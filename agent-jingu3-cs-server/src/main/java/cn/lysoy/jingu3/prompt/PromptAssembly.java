package cn.lysoy.jingu3.prompt;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.constant.PromptTemplates;
import org.springframework.stereotype.Component;

/**
 * 提示词拼装（ENG:提示词工程）：将 {@link PromptTemplates} 中的系统/角色文案与 {@link PromptFragments}
 * 中的换行、标签等拼成最终送入 LLM 的字符串。各 {@link cn.lysoy.jingu3.engine.mode} 处理器只依赖本类，
 * 避免模板散落在业务代码中；变更提示词时优先改 {@link PromptTemplates} 并同步设计文档。
 */
@Component
public class PromptAssembly {

    /**
     * Ask 系统提示（与 {@link PromptTemplates#ASK_SYSTEM} 一致，便于按模式取文案）。
     */
    public String askSystemPrompt() {
        return PromptTemplates.ASK_SYSTEM;
    }

    /**
     * ReAct 系统提示。
     */
    public String reactSystemPrompt() {
        return PromptTemplates.REACT_SYSTEM;
    }

    /**
     * Ask：系统提示 + 用户段落，供单次 generate。
     */
    public String buildAskCombinedPrompt(String userMessage) {
        return PromptTemplates.ASK_SYSTEM
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + userMessage;
    }

    /**
     * ReAct：系统提示 + 用户段落。
     */
    public String buildReactCombinedPrompt(String userMessage) {
        return PromptTemplates.REACT_SYSTEM
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + userMessage;
    }

    public String buildPlanAndExecutePlanPrompt(String llmInput) {
        return PromptTemplates.PLAN_AND_EXECUTE_PLAN_ONLY
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + llmInput;
    }

    public String buildPlanAndExecuteExecutePrompt(String planText, String originalUserMessage) {
        return PromptTemplates.PLAN_AND_EXECUTE_EXECUTE_HEADER
                + PromptFragments.PARAGRAPH_BREAK
                + "计划：\n"
                + planText
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + originalUserMessage;
    }

    public String buildWorkflowStep1Prompt(String llmInput) {
        return PromptTemplates.WORKFLOW_STEP1
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + llmInput;
    }

    public String buildWorkflowStep2Prompt(String summary, String originalUserMessage) {
        return PromptTemplates.WORKFLOW_STEP2_HEADER
                + PromptFragments.PARAGRAPH_BREAK
                + summary
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + originalUserMessage;
    }

    public String buildAgentTeamLeadPrompt(String llmInput) {
        return PromptTemplates.AGENT_TEAM_LEAD
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + llmInput;
    }

    public String buildAgentTeamSubPrompt(String subtask) {
        return PromptTemplates.AGENT_TEAM_SUB_PREFIX + subtask;
    }

    /**
     * ReAct 多步循环中单步提示（指南 §4：思考→行动→观察）。
     */
    public String buildReactLoopStepPrompt(String userMessage, String priorTrace, int stepIndex, int maxSteps) {
        return PromptTemplates.REACT_LOOP_INSTRUCTION
                + PromptFragments.PARAGRAPH_BREAK
                + "当前第 " + stepIndex + " 步（最多 " + maxSteps + " 步）。"
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + userMessage
                + PromptFragments.PARAGRAPH_BREAK
                + "已有过程：\n"
                + (priorTrace == null || priorTrace.isBlank() ? "（无）" : priorTrace);
    }

    public String buildSubtaskExecutePrompt(String subtask, String originalUserMessage, int stepNumber) {
        return PromptTemplates.SUBTASK_EXECUTE_HEADER
                + "（第 " + stepNumber + " 步）\n"
                + subtask
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + originalUserMessage;
    }

    public String buildReplannerPrompt(String failedPlan, String errorHint, String originalUserMessage) {
        return PromptTemplates.REPLANNER_HEADER
                + PromptFragments.PARAGRAPH_BREAK
                + "原计划：\n"
                + failedPlan
                + PromptFragments.PARAGRAPH_BREAK
                + "错误：" + errorHint
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + originalUserMessage;
    }

    public String buildWorkflowNodePrompt(String nodeInstruction, String accumulatedContext, String originalUserMessage) {
        return PromptTemplates.WORKFLOW_NODE_HEADER
                + nodeInstruction
                + PromptFragments.PARAGRAPH_BREAK
                + "当前上下文与中间结果：\n"
                + accumulatedContext
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + originalUserMessage;
    }
}
