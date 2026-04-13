package cn.lysoy.jingu3.engine.routing;

/**
 * 意图场景识别决策来源（优先级：客户端显式 &gt; 规则 &gt; 模型）。
 */
public enum RoutingSource {
    CLIENT_EXPLICIT,
    RULE,
    MODEL,
    /** 客户端显式 mode 无法解析时降级为 REACT */
    FALLBACK,
    /**
     * 客户端显式选择 Plan-and-Execute 或 Agent Team，经意图分类判定更适合 Ask/ReAct 时，实际执行降为 Ask。
     */
    EXPLICIT_GUARD
}
