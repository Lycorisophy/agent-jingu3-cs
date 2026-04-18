package cn.lysoy.jingu3.service.guard.routing;

/**
 * 意图场景识别中「本轮模式由谁拍板」的来源枚举，用于日志、指标与产品解释。
 * <strong>优先级</strong>（与 {@link IntentRouter} 一致）：客户端显式 &gt; 规则 &gt; 模型；无法解析显式 mode 时走 {@link #FALLBACK}；
 * 显式重模式被守门降级时走 {@link #EXPLICIT_GUARD}。
 */
public enum RoutingSource {
    /** 请求体中显式传入的 {@code mode}，在合法且未被守门改写时采用。 */
    CLIENT_EXPLICIT,
    /** 关键词/规则表命中，低成本、可解释。 */
    RULE,
    /** {@link ModelIntentClassifier} 调用成功且采用其输出。 */
    MODEL,
    /** 客户端显式 mode 无法解析时降级为 REACT */
    FALLBACK,
    /**
     * 客户端显式选择 Plan-and-Execute 或 Agent Team，经意图分类判定更适合 Ask/ReAct 时，实际执行降为 Ask。
     */
    EXPLICIT_GUARD
}
