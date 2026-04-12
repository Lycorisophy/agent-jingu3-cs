package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.engine.ActionMode;

/**
 * 路由结果，供日志与下游编排使用。
 */
public record RoutingDecision(ActionMode mode, RoutingSource source, String note) {
}
