package cn.lysoy.jingu3.service.mode.support;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.mode.workflow.WorkflowNode;
import cn.lysoy.jingu3.service.prompt.PromptAssembly;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import cn.lysoy.jingu3.skill.tool.ToolExecutionException;
import cn.lysoy.jingu3.skill.tool.ToolRegistry;
import cn.lysoy.jingu3.skill.tool.ToolRoutingParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

/**
 * <strong>工具化子步骤复用层</strong>（驾驭工程 × 技能与工具系统，v0.4+）：把「Ask 式一行 JSON 路由 → 可选
 * {@link ToolRegistry#execute} → 再 generate 汇总」抽成公共逻辑，供
 * <ul>
 *   <li>{@link cn.lysoy.jingu3.service.mode.handler.PlanAndExecuteModeHandler} 的 Plan 子任务；</li>
 *   <li>{@link cn.lysoy.jingu3.service.mode.handler.WorkflowModeHandler} 的 {@code TOOL} 节点。</li>
 * </ul>
 * 避免 Plan 与 Workflow 在工具路径上复制粘贴，降低 JSON 约定漂移风险。
 */
@Service
public class ToolStepService {

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;
    private final ToolRegistry toolRegistry;
    private final Jingu3Properties properties;
    private final ObjectMapper objectMapper;

    public ToolStepService(
            ChatLanguageModel chat,
            PromptAssembly prompts,
            ToolRegistry toolRegistry,
            Jingu3Properties properties,
            ObjectMapper objectMapper) {
        this.chat = chat;
        this.prompts = prompts;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行计划中的一步：可选工具路由 + 汇总；{@code sink} 非 null 且走工具时发 {@link StreamEventSink#toolResult}。
     */
    public String runPlanSubtask(
            ExecutionContext ctx,
            String subtask,
            String originalUserMessage,
            int stepNumber,
            StreamEventSink sink) {
        if (!properties.getTool().isEnabled()) {
            // 全局关工具：子任务退化为单次 LLM 直答
            return chat.generate(prompts.buildSubtaskExecutePrompt(ctx, subtask, originalUserMessage, stepNumber));
        }
        // 与 Ask 一致：先让模型输出一行 JSON 决定 direct 或 tool
        String routeRaw = chat.generate(prompts.buildPlanSubtaskToolRouterPrompt(ctx, subtask, originalUserMessage, stepNumber));
        ToolRoutingParser.AskRoutePayload route =
                ToolRoutingParser.parseAskRoute(routeRaw, objectMapper)
                        .orElse(ToolRoutingParser.AskRoutePayload.direct());
        if (!route.useTool()) {
            return chat.generate(prompts.buildSubtaskExecutePrompt(ctx, subtask, originalUserMessage, stepNumber));
        }
        try {
            String toolOut = toolRegistry.execute(route.toolId(), route.input());
            if (sink != null) {
                sink.toolResult(route.toolId(), toolOut);
            }
            return chat.generate(
                    prompts.buildPlanSubtaskAfterToolPrompt(
                            ctx, subtask, originalUserMessage, stepNumber, route.toolId(), toolOut));
        } catch (ToolExecutionException e) {
            return chat.generate(
                    prompts.buildPlanSubtaskToolFailurePrompt(
                            ctx, subtask, originalUserMessage, stepNumber, e.getMessage()));
        }
    }

    /**
     * 工作流 TOOL 节点：执行工具并将输出（或错误）拼入 {@code accumulated}。
     */
    public String runWorkflowToolNode(WorkflowNode node, String accumulated, StreamEventSink sink) {
        // TOOL 节点：toolId 必填；instruction 作为工具输入串（可为空）
        String toolId = node.getToolId();
        if (toolId == null || toolId.isBlank()) {
            return accumulated
                    + PromptFragments.PARAGRAPH_BREAK
                    + "[工作流 TOOL 节点缺少 toolId，已跳过工具执行]";
        }
        if (!properties.getTool().isEnabled()) {
            return accumulated
                    + PromptFragments.PARAGRAPH_BREAK
                    + "[工具子系统已关闭，跳过 TOOL 节点: "
                    + toolId
                    + "]";
        }
        String input = node.getInstruction() != null ? node.getInstruction() : "";
        try {
            String out = toolRegistry.execute(toolId, input);
            if (sink != null) {
                sink.toolResult(toolId, out);
            }
            return accumulated
                    + PromptFragments.PARAGRAPH_BREAK
                    + "[工具 "
                    + toolId
                    + " 输出]\n"
                    + out;
        } catch (ToolExecutionException e) {
            return accumulated
                    + PromptFragments.PARAGRAPH_BREAK
                    + "[工具 "
                    + toolId
                    + " 错误] "
                    + e.getMessage();
        }
    }
}
