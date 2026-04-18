package cn.lysoy.jingu3.service.mode.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.Map;

/**
 * LangGraph4j 共享状态：Workflow 线性节点链仅依赖累计字符串上下文。
 */
public class WorkflowGraphState extends AgentState {

    public static final String K_ACCUMULATED = "accumulated";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            K_ACCUMULATED,
            Channels.base((String a, String b) -> b == null ? a : b, () -> "")
    );

    public WorkflowGraphState(Map<String, Object> init) {
        super(init);
    }

    public String accumulated() {
        return value(K_ACCUMULATED).map(Object::toString).orElse("");
    }
}
