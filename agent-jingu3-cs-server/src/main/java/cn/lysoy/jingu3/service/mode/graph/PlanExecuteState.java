package cn.lysoy.jingu3.service.mode.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * LangGraph4j 共享状态：Plan-and-Execute 子任务循环（parse → exec_step* → finalize）。
 */
public class PlanExecuteState extends AgentState {

    public static final String K_PLAN = "plan";
    public static final String K_SUBTASKS = "subtasks";
    public static final String K_INDEX = "idx";
    /** 每步子任务输出（Appender） */
    public static final String K_OUTPUTS = "outputs";
    /** 最近一步子任务文本（便于流式 block） */
    public static final String K_LAST_BLOCK = "lastBlock";
    /** 同步路径最终汇总 */
    public static final String K_RESULT = "result";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            K_PLAN, Channels.base((String a, String b) -> b == null ? a : b, () -> ""),
            K_SUBTASKS, Channels.base((List<String> a, List<String> b) -> b == null ? a : b, ArrayList::new),
            K_INDEX, Channels.base((Integer a, Integer b) -> b == null ? a : b, () -> 0),
            K_OUTPUTS, Channels.appender(ArrayList::new),
            K_LAST_BLOCK, Channels.base((String a, String b) -> b == null ? a : b, () -> ""),
            K_RESULT, Channels.base((String a, String b) -> b == null ? a : b, () -> "")
    );

    public PlanExecuteState(Map<String, Object> init) {
        super(init);
    }

    public String plan() {
        return value(K_PLAN).map(Object::toString).orElse("");
    }

    @SuppressWarnings("unchecked")
    public List<String> subtasks() {
        return value(K_SUBTASKS)
                .map(o -> (List<String>) o)
                .orElseGet(List::of);
    }

    public int idx() {
        return value(K_INDEX).map(o -> ((Number) o).intValue()).orElse(0);
    }

    @SuppressWarnings("unchecked")
    public List<String> outputs() {
        return value(K_OUTPUTS)
                .map(o -> (List<String>) o)
                .orElseGet(ArrayList::new);
    }

    public String lastBlock() {
        return value(K_LAST_BLOCK).map(Object::toString).orElse("");
    }

    public String result() {
        return value(K_RESULT).map(Object::toString).orElse("");
    }
}
