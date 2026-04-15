package cn.lysoy.jingu3.prompt;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.constant.PromptTemplates;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.tool.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * <strong>提示词工程（ENG: Prompt Engineering）</strong>在服务端的中枢：将 {@link PromptTemplates} 中按模式维护的
 * 系统/角色/子任务模板与 {@link PromptFragments} 中的换行、用户标签等拼接为<strong>完整送模串</strong>。
 * <p>
 * <strong>与上下文工程的关系</strong>：本类不负责记忆检索或 UTC/平台行（见 {@link UserPromptPreparationService}），
 * 仅假设 {@link ExecutionContext#getUserMessage()} / {@link ExecutionContext#llmInput()} 已是「路由与业务上希望模型看到」的文本；
 * 每条提示均在段首通过 {@link ModeRoutingPreamble} 注入<strong>意图与模式可见性</strong>说明，满足指南与客户端规范对 LLM 透明度的要求。
 * </p>
 * <p>
 * <strong>与工具系统的关系</strong>：Ask/ReAct/Plan 子任务/Agent Team 专员等路径按需拼接 {@link ToolRegistry#buildCatalogMarkdown()}，
 * 与 {@link cn.lysoy.jingu3.tool.ToolRoutingParser} 产出的 JSON 约定闭合。
 * </p>
 */
@Component
public class PromptAssembly {

    /** 内置工具目录 Markdown，注入到「工具路由」类提示中 */
    private final ToolRegistry toolRegistry;
    /** 是否启用工具、子 Agent 轮次等运行时开关 */
    private final Jingu3Properties properties;

    public PromptAssembly(ToolRegistry toolRegistry, Jingu3Properties properties) {
        this.toolRegistry = toolRegistry;
        this.properties = properties;
    }

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

    /** 统一在正文前拼接「模式路由说明 + 各模式速查」，避免各 build* 方法遗漏 */
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

    /** Ask：首轮工具路由（仅输出一行 JSON） */
    public String buildAskToolRouterPrompt(ExecutionContext ctx) {
        return withModePreamble(
                ctx,
                PromptTemplates.ASK_TOOL_ROUTER_INSTRUCTION
                        + toolRegistry.buildCatalogMarkdown()
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + ctx.llmInput());
    }

    /** Ask：工具成功后汇总 */
    public String buildAskAfterToolPrompt(ExecutionContext ctx, String toolId, String toolOutput) {
        return withModePreamble(
                ctx,
                PromptTemplates.ASK_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + "用户问题：\n"
                        + ctx.llmInput()
                        + PromptFragments.PARAGRAPH_BREAK
                        + "工具 `"
                        + toolId
                        + "` 输出：\n"
                        + toolOutput
                        + PromptFragments.PARAGRAPH_BREAK
                        + "请基于上述工具输出（若与常识冲突以工具为准）用中文简洁作答。");
    }

    /** Ask：工具失败时仍给模型一次解释机会 */
    public String buildAskToolFailurePrompt(ExecutionContext ctx, String errorMessage) {
        return withModePreamble(
                ctx,
                PromptTemplates.ASK_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + "用户问题：\n"
                        + ctx.llmInput()
                        + PromptFragments.PARAGRAPH_BREAK
                        + "工具调用失败："
                        + errorMessage
                        + PromptFragments.PARAGRAPH_BREAK
                        + "请用中文说明失败原因并尽量给出可操作的替代建议（如改写表达式）。");
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

    /** 指南 §5 第一阶段：仅产出编号计划，不执行工具 */
    public String buildPlanAndExecutePlanPrompt(ExecutionContext ctx) {
        return withModePreamble(
                ctx,
                PromptTemplates.PLAN_AND_EXECUTE_PLAN_ONLY
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + ctx.llmInput());
    }

    /**
     * 指南 §5 第二阶段：在已定计划下执行子任务（由 {@link cn.lysoy.jingu3.engine.mode.PlanAndExecuteModeHandler} 循环调用）。
     */
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

    /** 指南 §6：无 JSON 定义时的两步固定工作流 — 第一步（概括用户目标） */
    public String buildWorkflowStep1Prompt(ExecutionContext ctx, String llmInput) {
        return withModePreamble(
                ctx,
                PromptTemplates.WORKFLOW_STEP1
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + llmInput);
    }

    /** 指南 §6：两步固定工作流 — 第二步（在概括基础上作答） */
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
        return withModePreamble(
                ctx,
                PromptTemplates.AGENT_TEAM_SUB_BOUNDARY
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptTemplates.AGENT_TEAM_SUB_PREFIX
                        + subtask);
    }

    public String buildAgentTeamSubFollowUpPrompt(ExecutionContext ctx, String subtask, String priorRoundsText) {
        return withModePreamble(
                ctx,
                PromptTemplates.AGENT_TEAM_SUB_BOUNDARY
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptTemplates.AGENT_TEAM_SUB_FOLLOWUP
                        + subtask
                        + PromptFragments.PARAGRAPH_BREAK
                        + "此前各轮输出：\n"
                        + priorRoundsText);
    }

    /** 专员轮：供工具路由与直答的「用户侧」段落（首轮，含原始用户问题）。 */
    public String buildAgentTeamSpecialistUserParagraphRound1(ExecutionContext ctx, String leaderSubtask) {
        return PromptTemplates.AGENT_TEAM_SUB_PREFIX
                + leaderSubtask
                + PromptFragments.PARAGRAPH_BREAK
                + "用户原始问题：\n"
                + ctx.llmInput();
    }

    /** 专员轮：供工具路由与直答的「用户侧」段落（后续轮）。 */
    public String buildAgentTeamSpecialistUserParagraphFollowUp(
            ExecutionContext ctx, String leaderSubtask, String priorRoundsText) {
        return PromptTemplates.AGENT_TEAM_SUB_FOLLOWUP
                + leaderSubtask
                + PromptFragments.PARAGRAPH_BREAK
                + "此前各轮输出：\n"
                + priorRoundsText
                + PromptFragments.PARAGRAPH_BREAK
                + "用户原始问题：\n"
                + ctx.llmInput();
    }

    /** 专员轮：Ask 同款 JSON 工具路由提示（含子 Agent 边界）。 */
    public String buildAgentTeamSpecialistToolRouterPrompt(ExecutionContext ctx, String specialistUserParagraph) {
        return withModePreamble(
                ctx,
                PromptTemplates.AGENT_TEAM_SUB_BOUNDARY
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptTemplates.ASK_TOOL_ROUTER_INSTRUCTION
                        + toolRegistry.buildCatalogMarkdown()
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + specialistUserParagraph);
    }

    /** 专员轮：不调工具时的单次作答提示。 */
    public String buildAgentTeamSpecialistDirectPrompt(ExecutionContext ctx, String specialistUserParagraph) {
        return withModePreamble(
                ctx,
                PromptTemplates.AGENT_TEAM_SUB_BOUNDARY
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptTemplates.ASK_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + specialistUserParagraph);
    }

    /** 专员轮：工具成功后的汇总提示。 */
    public String buildAgentTeamSpecialistAfterToolPrompt(
            ExecutionContext ctx, String specialistUserParagraph, String toolId, String toolOutput) {
        return withModePreamble(
                ctx,
                PromptTemplates.AGENT_TEAM_SUB_BOUNDARY
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptTemplates.ASK_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + "子 Agent 待完成内容：\n"
                        + specialistUserParagraph
                        + PromptFragments.PARAGRAPH_BREAK
                        + "工具 `"
                        + toolId
                        + "` 输出：\n"
                        + toolOutput
                        + PromptFragments.PARAGRAPH_BREAK
                        + "请仅输出中文正文结果，并遵守上文【子 Agent 边界】。");
    }

    /** 专员轮：工具失败时的说明提示。 */
    public String buildAgentTeamSpecialistToolFailurePrompt(
            ExecutionContext ctx, String specialistUserParagraph, String errorMessage) {
        return withModePreamble(
                ctx,
                PromptTemplates.AGENT_TEAM_SUB_BOUNDARY
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptTemplates.ASK_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + "子 Agent 待完成内容：\n"
                        + specialistUserParagraph
                        + PromptFragments.PARAGRAPH_BREAK
                        + "工具调用失败："
                        + errorMessage
                        + PromptFragments.PARAGRAPH_BREAK
                        + "请用中文说明原因并尽量给出可操作的替代建议，并遵守上文【子 Agent 边界】。");
    }

    /** 指南 §7：Leader 子任务 + 各专员轮输出 → 合成对用户可见的最终答复 */
    public String buildAgentTeamSynthesizePrompt(ExecutionContext ctx, String leaderSubtask, String trajectoryText) {
        return withModePreamble(
                ctx,
                PromptTemplates.AGENT_TEAM_SYNTHESIZE
                        + "子任务：\n"
                        + leaderSubtask
                        + PromptFragments.PARAGRAPH_BREAK
                        + "子 Agent 输出：\n"
                        + trajectoryText);
    }

    /**
     * ReAct 多步循环中单步提示（指南 §4：思考→行动→观察）。
     */
    public String buildReactLoopStepPrompt(
            ExecutionContext ctx, String userMessage, String priorTrace, int stepIndex, int maxSteps) {
        String toolBlock = "";
        if (properties.getTool().isEnabled()) {
            toolBlock =
                    PromptFragments.PARAGRAPH_BREAK
                            + "可用工具列表：\n"
                            + toolRegistry.buildCatalogMarkdown()
                            + PromptFragments.PARAGRAPH_BREAK
                            + PromptTemplates.REACT_JSON_FOOTER_RULE;
        }
        return withModePreamble(
                ctx,
                PromptTemplates.REACT_LOOP_INSTRUCTION
                        + toolBlock
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

    /** Plan 子任务直答（不走路由 JSON 时）的单步执行提示 */
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

    /** Plan 执行失败时的最小 Replanner 提示（单次重规划入口） */
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

    /** classpath JSON 工作流：单个 LLM 节点上的指令与用户上下文拼接 */
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

    /** Plan 子任务：工具路由（仅一行 JSON） */
    public String buildPlanSubtaskToolRouterPrompt(
            ExecutionContext ctx, String subtask, String originalUserMessage, int stepNumber) {
        return withModePreamble(
                ctx,
                PromptTemplates.PLAN_SUBTASK_TOOL_ROUTER_INSTRUCTION
                        + toolRegistry.buildCatalogMarkdown()
                        + PromptFragments.PARAGRAPH_BREAK
                        + "子任务（第 "
                        + stepNumber
                        + " 步）：\n"
                        + subtask
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + originalUserMessage);
    }

    /** Plan 子任务：工具成功后汇总为该步结果 */
    public String buildPlanSubtaskAfterToolPrompt(
            ExecutionContext ctx,
            String subtask,
            String originalUserMessage,
            int stepNumber,
            String toolId,
            String toolOutput) {
        return withModePreamble(
                ctx,
                PromptTemplates.ASK_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptTemplates.SUBTASK_EXECUTE_HEADER
                        + "（第 "
                        + stepNumber
                        + " 步）\n"
                        + subtask
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + originalUserMessage
                        + PromptFragments.PARAGRAPH_BREAK
                        + "工具 `"
                        + toolId
                        + "` 输出：\n"
                        + toolOutput
                        + PromptFragments.PARAGRAPH_BREAK
                        + "请基于工具输出完成该子任务，用中文简洁给出本步结果。");
    }

    /** Plan 子任务：工具失败时说明并完成该步 */
    public String buildPlanSubtaskToolFailurePrompt(
            ExecutionContext ctx,
            String subtask,
            String originalUserMessage,
            int stepNumber,
            String errorMessage) {
        return withModePreamble(
                ctx,
                PromptTemplates.ASK_SYSTEM
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptTemplates.SUBTASK_EXECUTE_HEADER
                        + "（第 "
                        + stepNumber
                        + " 步）\n"
                        + subtask
                        + PromptFragments.PARAGRAPH_BREAK
                        + PromptFragments.USER_LABEL_WITH_NEWLINE
                        + originalUserMessage
                        + PromptFragments.PARAGRAPH_BREAK
                        + "工具调用失败："
                        + errorMessage
                        + PromptFragments.PARAGRAPH_BREAK
                        + "请用中文说明失败原因并尽量给出本步可交付的替代结果。");
    }
}
