package cn.lysoy.jingu3.engine.routing;

/**
 * 意图场景识别决策来源（优先级：客户端显式 &gt; 规则 &gt; 模型）。
 */
public enum RoutingSource {
    CLIENT_EXPLICIT,
    RULE,
    MODEL
}
