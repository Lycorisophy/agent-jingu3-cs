package cn.lysoy.jingu3.service.mode.graph;

import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.mode.plan.PlanTextParser;
import cn.lysoy.jingu3.service.mode.support.ToolStepService;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Plan-and-Execute 的 LangGraph4j 编排：parse →（条件）exec_step 循环 → finalize。
 */
public final class PlanExecuteGraphSupport {

    private PlanExecuteGraphSupport() {
    }

    public static String invokeSync(
            ExecutionContext ctx,
            String plan,
            String originalUserMessage,
            ToolStepService toolStepService,
            int maxSubtasks) {
        try {
            CompiledPlanGraph g = buildCompiled(ctx, originalUserMessage, toolStepService, maxSubtasks, null, null);
            return g.compiled
                    .invoke(GraphInput.args(Map.of(PlanExecuteState.K_PLAN, plan)), g.config)
                    .map(s -> ((PlanExecuteState) s).result())
                    .orElse("").toString();
        } catch (GraphStateException e) {
            throw new IllegalStateException("Plan-and-Execute LangGraph 执行失败", e);
        }
    }

    /**
     * 流式：plan 步已由 Handler 推送；本图从 parse 起，子任务步号由 {@code stepCounter} 递增（与调用方 replan 衔接）。
     */
    public static void invokeStream(
            ExecutionContext ctx,
            String plan,
            String originalUserMessage,
            ToolStepService toolStepService,
            int maxSubtasks,
            StreamEventSink sink,
            AtomicInteger stepCounter) {
        try {
            CompiledPlanGraph g = buildCompiled(ctx, originalUserMessage, toolStepService, maxSubtasks, sink, stepCounter);
            g.compiled.invoke(
                    GraphInput.args(Map.of(PlanExecuteState.K_PLAN, plan)),
                    g.config);
        } catch (GraphStateException e) {
            throw new IllegalStateException("Plan-and-Execute LangGraph 流式执行失败", e);
        }
    }

    private static CompiledPlanGraph buildCompiled(
            ExecutionContext ctx,
            String originalUserMessage,
            ToolStepService toolStepService,
            int maxSubtasks,
            StreamEventSink sinkOrNull,
            AtomicInteger stepCounterOrNull) throws GraphStateException {

        NodeAction<PlanExecuteState> parse = state -> {
            String p = state.plan();
            List<String> subs = PlanTextParser.parseSubtasks(p);
            List<String> use = subs;
            if (use.size() > maxSubtasks) {
                use = new ArrayList<>(subs.subList(0, maxSubtasks));
            }
            Map<String, Object> u = new HashMap<>();
            u.put(PlanExecuteState.K_SUBTASKS, new ArrayList<>(use));
            u.put(PlanExecuteState.K_INDEX, 0);
            return u;
        };

        NodeAction<PlanExecuteState> execStep = state -> {
            List<String> st = state.subtasks();
            int i = state.idx();
            if (i >= st.size()) {
                return Map.of();
            }
            String task = st.get(i);
            int step = 0;
            if (sinkOrNull != null && stepCounterOrNull != null) {
                step = stepCounterOrNull.getAndIncrement();
                sinkOrNull.stepBegin(step, "subtask_" + (i + 1));
            }
            String r = toolStepService.runPlanSubtask(ctx, task, originalUserMessage, i + 1, sinkOrNull);
            String block = r == null ? "" : r;
            if (sinkOrNull != null) {
                sinkOrNull.block(block);
                sinkOrNull.stepEnd(step);
            }
            Map<String, Object> u = new HashMap<>();
            u.put(PlanExecuteState.K_INDEX, i + 1);
            u.put(PlanExecuteState.K_OUTPUTS, block);
            u.put(PlanExecuteState.K_LAST_BLOCK, block);
            return u;
        };

        NodeAction<PlanExecuteState> finalize = state -> {
            String p = state.plan();
            List<String> outs = new ArrayList<>(state.outputs());
            StringBuilder sb = new StringBuilder();
            sb.append("【计划】\n").append(p).append("\n\n【分步结果】\n");
            for (int j = 0; j < outs.size(); j++) {
                sb.append("\n---\n步骤 ").append(j + 1).append(":\n").append(outs.get(j));
            }
            return Map.of(PlanExecuteState.K_RESULT, sb.toString());
        };

        EdgeAction afterParse = state -> state.subtasks().isEmpty() ? "empty" : "run";

        EdgeAction afterExec = state -> state.idx() < state.subtasks().size() ? "again" : "done";

        StateGraph graph = new StateGraph(PlanExecuteState.SCHEMA, PlanExecuteState::new)
                .addNode("parse", node_async(parse))
                .addNode("exec_step", node_async(execStep))
                .addNode("finalize", node_async(finalize))
                .addEdge(START, "parse")
                .addConditionalEdges("parse", edge_async(afterParse), Map.of("empty", "finalize", "run", "exec_step"))
                .addConditionalEdges("exec_step", edge_async(afterExec), Map.of("again", "exec_step", "done", "finalize"))
                .addEdge("finalize", END);

        RunnableConfig cfg = RunnableConfig.builder().build();
        CompiledGraph compiled = graph.compile(CompileConfig.builder().recursionLimit(64).build());
        return new CompiledPlanGraph(compiled, cfg);
    }

    private record CompiledPlanGraph(CompiledGraph compiled, RunnableConfig config) {
    }
}
