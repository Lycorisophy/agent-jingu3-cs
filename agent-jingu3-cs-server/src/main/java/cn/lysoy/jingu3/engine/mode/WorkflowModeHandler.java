package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.support.ToolStepService;
import cn.lysoy.jingu3.engine.workflow.WorkflowDefinition;
import cn.lysoy.jingu3.engine.workflow.WorkflowDefinitionRegistry;
import cn.lysoy.jingu3.engine.workflow.WorkflowNode;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.stream.StreamEventSink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 指南 §6 Workflow：按预置流程（本仓库为 classpath {@code workflows/*.json} 顺序节点）逐段执行；
 * {@code LLM} 节点走提示链，{@code TOOL} 节点经 {@link cn.lysoy.jingu3.engine.support.ToolStepService} 调用
 * {@link cn.lysoy.jingu3.tool.ToolRegistry} 并将输出拼入上下文。
 * 无定义或空节点时回退为历史「摘要 → 方案」两步，与早期固定两阶段行为兼容。
 * <p>流式：每节点一次 BLOCK，STEP 标签优先使用节点 {@link WorkflowNode#getId()}；TOOL 节点可先发 {@code TOOL_RESULT}。</p>
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

    /**
     * 若存在工作流定义则顺序执行节点；否则走 {@link #fallbackTwoStep}。
     */
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
            return fallbackTwoStep(context, in, context.getUserMessage());
        }

        // 顺序链：每节点把「节点指令 + 已累计的中间结果 + 原始用户话」交给模型，形成链式上下文
        String accumulated = in;
        for (WorkflowNode node : def.getNodes()) {
            if (node.isToolNode()) {
                accumulated = toolStepService.runWorkflowToolNode(node, accumulated, null);
            } else {
                String instr = node.getInstruction() != null ? node.getInstruction() : "";
                accumulated = chat.generate(
                        prompts.buildWorkflowNodePrompt(context, instr, accumulated, context.getUserMessage()));
            }
        }
        return accumulated;
    }

    /**
     * 流式：每节点输出当前累计上下文（与同步最终返回值在最后一节点上语义一致）；回退路径为两步 BLOCK。
     */
    public void stream(ExecutionContext context, StreamEventSink sink) {
        String wf = context.getWorkflowId();
        String header = (wf != null && !wf.isBlank())
                ? "[workflowId=" + wf + "]" + PromptFragments.PARAGRAPH_BREAK
                : "";
        String in = header + context.llmInput();

        WorkflowDefinition def = workflowDefinitionRegistry.get(wf);
        if (def == null || def.getNodes() == null || def.getNodes().isEmpty()) {
            log.warn("workflow definition not found for id={}, fallback two-step stream", wf);
            streamFallbackTwoStep(context, in, context.getUserMessage(), sink);
            return;
        }

        String accumulated = in;
        int step = 1;
        for (WorkflowNode node : def.getNodes()) {
            String nid = node.getId() != null ? node.getId() : "node_" + step;
            sink.stepBegin(step, nid);
            if (node.isToolNode()) {
                accumulated = toolStepService.runWorkflowToolNode(node, accumulated, sink);
            } else {
                String instr = node.getInstruction() != null ? node.getInstruction() : "";
                accumulated = chat.generate(
                        prompts.buildWorkflowNodePrompt(context, instr, accumulated, context.getUserMessage()));
            }
            sink.block(accumulated == null ? "" : accumulated);
            sink.stepEnd(step);
            step++;
        }
        sink.done();
    }

    private void streamFallbackTwoStep(
            ExecutionContext context, String llmInput, String originalUserMessage, StreamEventSink sink) {
        sink.stepBegin(1, "workflow_fallback_1");
        String summary = chat.generate(prompts.buildWorkflowStep1Prompt(context, llmInput));
        sink.block(summary == null ? "" : summary);
        sink.stepEnd(1);
        sink.stepBegin(2, "workflow_fallback_2");
        String out = chat.generate(prompts.buildWorkflowStep2Prompt(context, summary, originalUserMessage));
        sink.block(out == null ? "" : out);
        sink.stepEnd(2);
        sink.done();
    }

    private String fallbackTwoStep(ExecutionContext context, String llmInput, String originalUserMessage) {
        String summary = chat.generate(prompts.buildWorkflowStep1Prompt(context, llmInput));
        return chat.generate(prompts.buildWorkflowStep2Prompt(context, summary, originalUserMessage));
    }
}
