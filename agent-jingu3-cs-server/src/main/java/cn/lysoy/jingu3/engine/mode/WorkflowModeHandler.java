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
 * <strong>指南 §6 Workflow</strong>（八大行动模式之一）：按预置<strong>有向节点链</strong>执行（本仓库为 classpath
 * {@code workflows/*.json}，由 {@link WorkflowDefinitionRegistry} 加载）。节点类型：{@code LLM} 走
 * {@link PromptAssembly#buildWorkflowNodePrompt}；{@code TOOL} 经 {@link ToolStepService#runWorkflowToolNode}
 * 调 {@link cn.lysoy.jingu3.tool.ToolRegistry}，输出拼入累计上下文供后续节点消费。
 * <p>无定义或空节点时回退为「摘要 → 方案」两步固定提示，兼容早期无 JSON 工作流场景。</p>
 * <p><strong>流式</strong>：每节点一步 stepBegin/block/stepEnd；TOOL 可在 block 前 toolResult。</p>
 */
@Slf4j
@Component
public class WorkflowModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;
    /** workflowId → 节点链定义（进程内缓存） */
    private final WorkflowDefinitionRegistry workflowDefinitionRegistry;
    /** TOOL 节点与 Plan 子任务共享工具执行与提示拼装 */
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

        // 顺序链：accumulated 携带「入口输入 + 各节点产出」，供下游 LLM/TOOL 作为单一上下文串消费
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
