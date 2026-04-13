package cn.lysoy.jingu3.engine.support;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.workflow.WorkflowNode;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.stream.StreamEventSink;
import cn.lysoy.jingu3.tool.ToolExecutionException;
import cn.lysoy.jingu3.tool.ToolRegistry;
import cn.lysoy.jingu3.tool.ToolRoutingParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Plan 子任务与工作流 TOOL 节点的工具路由与执行复用层（v0.4）。
 */
@Component
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
            return chat.generate(prompts.buildSubtaskExecutePrompt(ctx, subtask, originalUserMessage, stepNumber));
        }
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
