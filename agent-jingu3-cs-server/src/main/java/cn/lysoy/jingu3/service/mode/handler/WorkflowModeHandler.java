package cn.lysoy.jingu3.service.mode.handler;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.service.mode.ActionModeHandler;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.mode.graph.WorkflowGraphSupport;
import cn.lysoy.jingu3.service.mode.support.ToolStepService;
import cn.lysoy.jingu3.service.mode.workflow.WorkflowDefinition;
import cn.lysoy.jingu3.service.mode.workflow.WorkflowDefinitionRegistry;
import cn.lysoy.jingu3.service.prompt.PromptAssembly;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <strong>指南 §6 Workflow</strong>（八大行动模式之一）：按预置<strong>有向节点链</strong>执行（classpath {@code workflows/*.json}）。
 * 编排由 LangGraph4j 线性状态图实现（每 JSON 节点对应图上一节点）；无定义时回退为两步固定提示图。
 */
@Slf4j
@Component
public class WorkflowModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;
    private final WorkflowDefinitionRegistry workflowDefinitionRegistry;
    private final ToolStepService toolStepService;

    public WorkflowModeHandler(
            ChatLanguageModel chat,
            PromptAssembly prompts,
            WorkflowDefinitionRegistry workflowDefinitionRegistry,
            ToolStepService toolStepService) {
        this.chat = chat;
        this.prompts = prompts;
        this.workflowDefinitionRegistry = workflowDefinitionRegistry;
        this.toolStepService = toolStepService;
    }

    @Override
    public String execute(ExecutionContext context) {
        String wf = context.getWorkflowId();
        String header = (wf != null && !wf.isBlank())
                ? "[workflowId=" + wf + "]" + PromptFragments.PARAGRAPH_BREAK
                : "";
        String in = header + context.llmInput();

        WorkflowDefinition def = workflowDefinitionRegistry.get(wf);
        if (def == null || def.getNodes() == null || def.getNodes().isEmpty()) {
            log.warn("workflow definition not found for id={}, fallback two-step", wf);
            return WorkflowGraphSupport.invokeFallbackSync(context, in, chat, prompts);
        }

        return WorkflowGraphSupport.invokeLinearSync(context, def, in, chat, prompts, toolStepService);
    }

    public void stream(ExecutionContext context, StreamEventSink sink) {
        String wf = context.getWorkflowId();
        String header = (wf != null && !wf.isBlank())
                ? "[workflowId=" + wf + "]" + PromptFragments.PARAGRAPH_BREAK
                : "";
        String in = header + context.llmInput();

        WorkflowDefinition def = workflowDefinitionRegistry.get(wf);
        if (def == null || def.getNodes() == null || def.getNodes().isEmpty()) {
            log.warn("workflow definition not found for id={}, fallback two-step stream", wf);
            WorkflowGraphSupport.invokeFallbackStream(context, in, chat, prompts, sink);
            sink.done();
            return;
        }

        WorkflowGraphSupport.invokeLinearStream(context, def, in, chat, prompts, toolStepService, sink);
        sink.done();
    }

    /*
     * LEGACY pre-LangGraph4j — kept for reference & rollback (2026-04)
     *
     * public String execute(ExecutionContext context) {
     *     ...
     *     String accumulated = in;
     *     for (WorkflowNode node : def.getNodes()) {
     *         if (node.isToolNode()) {
     *             accumulated = toolStepService.runWorkflowToolNode(node, accumulated, null);
     *         } else {
     *             String instr = node.getInstruction() != null ? node.getInstruction() : "";
     *             accumulated = chat.generate(
     *                     prompts.buildWorkflowNodePrompt(context, instr, accumulated, context.getUserMessage()));
     *         }
     *     }
     *     return accumulated;
     * }
     *
     * public void stream(ExecutionContext context, StreamEventSink sink) {
     *     ...
     *     String accumulated = in;
     *     int step = 1;
     *     for (WorkflowNode node : def.getNodes()) {
     *         String nid = node.getId() != null ? node.getId() : "node_" + step;
     *         sink.stepBegin(step, nid);
     *         if (node.isToolNode()) {
     *             accumulated = toolStepService.runWorkflowToolNode(node, accumulated, sink);
     *         } else {
     *             String instr = node.getInstruction() != null ? node.getInstruction() : "";
     *             accumulated = chat.generate(
     *                     prompts.buildWorkflowNodePrompt(context, instr, accumulated, context.getUserMessage()));
     *         }
     *         sink.block(accumulated == null ? "" : accumulated);
     *         sink.stepEnd(step);
     *         step++;
     *     }
     *     sink.done();
     * }
     *
     * private void streamFallbackTwoStep(...) { ... }
     * private String fallbackTwoStep(...) { ... }
     */
}
