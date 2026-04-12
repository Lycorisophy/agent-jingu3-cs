package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.engine.ActionMode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 路由结果，供日志与下游编排使用。
 */
@Getter
@AllArgsConstructor
public class RoutingDecision {

    private final ActionMode mode;
    private final RoutingSource source;
    private final String note;
}
