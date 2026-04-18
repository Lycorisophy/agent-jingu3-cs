package cn.lysoy.jingu3.service.mode.graph;

import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.mode.support.ToolStepService;
import cn.lysoy.jingu3.service.mode.workflow.WorkflowDefinition;
import cn.lysoy.jingu3.service.mode.workflow.WorkflowNode;
import cn.lysoy.jingu3.service.prompt.PromptAssembly;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Workflow 线性节点链的 LangGraph4j 编排：按 JSON 定义顺序一节一点，或两步回退图。
 */
public final class WorkflowGraphSupport {

    private WorkflowGraphSupport() {
    }

    public static String invokeLinearSync(
            ExecutionContext context,
            WorkflowDefinition def,
            String initialAccumulated,
            ChatLanguageModel chat,
            PromptAssembly prompts,
            ToolStepService toolStepService) {
        try {
            CompiledGraph compiled = buildLinearGraph(
                    context, def.getNodes(), chat, prompts, toolStepService, null);
            return compiled
                    .invoke(GraphInput.args(Map.of(WorkflowGraphState.K_ACCUMULATED, initialAccumulated)),
                            RunnableConfig.builder().build())
                    .map(s -> ((WorkflowGraphState) s).accumulated())
                    .orElse("").toString();
        } catch (GraphStateException e) {
            throw new IllegalStateException("Workflow LangGraph 执行失败", e);
        }
    }

    public static void invokeLinearStream(
            ExecutionContext context,
            WorkflowDefinition def,
            String initialAccumulated,
            ChatLanguageModel chat,
            PromptAssembly prompts,
            ToolStepService toolStepService,
            StreamEventSink sink) {
        try {
            CompiledGraph compiled = buildLinearGraph(
                    context, def.getNodes(), chat, prompts, toolStepService, sink);
            compiled.invoke(
                    GraphInput.args(Map.of(WorkflowGraphState.K_ACCUMULATED, initialAccumulated)),
                    RunnableConfig.builder().build());
        } catch (GraphStateException e) {
            throw new IllegalStateException("Workflow LangGraph 流式执行失败", e);
        }
    }

    public static String invokeFallbackSync(
            ExecutionContext context,
            String llmInput,
            ChatLanguageModel chat,
            PromptAssembly prompts) {
        try {
            CompiledGraph compiled = buildFallbackGraph(context, chat, prompts, null);
            return compiled
                    .invoke(GraphInput.args(Map.of(WorkflowGraphState.K_ACCUMULATED, llmInput)),
                            RunnableConfig.builder().build())
                    .map(s -> ((WorkflowGraphState) s).accumulated())
                    .orElse("").toString();
        } catch (GraphStateException e) {
            throw new IllegalStateException("Workflow 回退 LangGraph 执行失败", e);
        }
    }

    public static void invokeFallbackStream(
            ExecutionContext context,
            String llmInput,
            ChatLanguageModel chat,
            PromptAssembly prompts,
            StreamEventSink sink) {
        try {
            CompiledGraph compiled = buildFallbackGraph(context, chat, prompts, sink);
            compiled.invoke(
                    GraphInput.args(Map.of(WorkflowGraphState.K_ACCUMULATED, llmInput)),
                    RunnableConfig.builder().build());
        } catch (GraphStateException e) {
            throw new IllegalStateException("Workflow 回退 LangGraph 流式执行失败", e);
        }
    }

    private static CompiledGraph buildLinearGraph(
            ExecutionContext context,
            List<WorkflowNode> nodes,
            ChatLanguageModel chat,
            PromptAssembly prompts,
            ToolStepService toolStepService,
            StreamEventSink sinkOrNull) throws GraphStateException {

        StateGraph graph = new StateGraph(WorkflowGraphState.SCHEMA, WorkflowGraphState::new);
        String prev = START;
        int step = 1;
        for (WorkflowNode node : nodes) {
            final WorkflowNode wn = node;
            String graphNodeId = "wf_" + step;
            String stepLabel = wn.getId() != null ? wn.getId() : "node_" + step;
            int stepNum = step;
            NodeAction<WorkflowGraphState> action = state -> {
                String acc = state.accumulated();
                if (sinkOrNull != null) {
                    sinkOrNull.stepBegin(stepNum, stepLabel);
                }
                String nextAcc;
                if (wn.isToolNode()) {
                    nextAcc = toolStepService.runWorkflowToolNode(wn, acc, sinkOrNull);
                } else {
                    String instr = wn.getInstruction() != null ? wn.getInstruction() : "";
                    nextAcc = chat.generate(
                            prompts.buildWorkflowNodePrompt(context, instr, acc, context.getUserMessage()));
                }
                if (nextAcc == null) {
                    nextAcc = "";
                }
                if (sinkOrNull != null) {
                    sinkOrNull.block(nextAcc);
                    sinkOrNull.stepEnd(stepNum);
                }
                return Map.of(WorkflowGraphState.K_ACCUMULATED, nextAcc);
            };
            graph.addNode(graphNodeId, node_async(action));
            graph.addEdge(prev, graphNodeId);
            prev = graphNodeId;
            step++;
        }
        graph.addEdge(prev, END);
        return graph.compile(CompileConfig.builder().recursionLimit(32).build());
    }

    private static CompiledGraph buildFallbackGraph(
            ExecutionContext context,
            ChatLanguageModel chat,
            PromptAssembly prompts,
            StreamEventSink sinkOrNull) throws GraphStateException {

        NodeAction<WorkflowGraphState> fb1 = state -> {
            String in = state.accumulated();
            if (sinkOrNull != null) {
                sinkOrNull.stepBegin(1, "workflow_fallback_1");
            }
            String summary = chat.generate(prompts.buildWorkflowStep1Prompt(context, in));
            if (summary == null) {
                summary = "";
            }
            if (sinkOrNull != null) {
                sinkOrNull.block(summary);
                sinkOrNull.stepEnd(1);
            }
            return Map.of(WorkflowGraphState.K_ACCUMULATED, summary);
        };

        NodeAction<WorkflowGraphState> fb2 = state -> {
            String summary = state.accumulated();
            if (sinkOrNull != null) {
                sinkOrNull.stepBegin(2, "workflow_fallback_2");
            }
            String out = chat.generate(prompts.buildWorkflowStep2Prompt(context, summary, context.getUserMessage()));
            if (out == null) {
                out = "";
            }
            if (sinkOrNull != null) {
                sinkOrNull.block(out);
                sinkOrNull.stepEnd(2);
            }
            return Map.of(WorkflowGraphState.K_ACCUMULATED, out);
        };

        StateGraph graph = new StateGraph(WorkflowGraphState.SCHEMA, WorkflowGraphState::new)
                .addNode("fb1", node_async(fb1))
                .addNode("fb2", node_async(fb2))
                .addEdge(START, "fb1")
                .addEdge("fb1", "fb2")
                .addEdge("fb2", END);
        return graph.compile(CompileConfig.builder().recursionLimit(8).build());
    }
}
