package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.engine.ActionMode;
import lombok.Getter;

/**
 * 路由结果，供日志与下游编排使用。
 */
@Getter
public class RoutingDecision {

    private final ActionMode mode;
    private final RoutingSource source;
    private final String note;
    /**
     * 显式 Plan/Agent Team 守门降级为 Ask 时，展示给用户的前缀说明；其它情况为 {@code null}。
     */
    private final String guardUserNotice;

    public RoutingDecision(ActionMode mode, RoutingSource source, String note) {
        this(mode, source, note, null);
    }

    public RoutingDecision(ActionMode mode, RoutingSource source, String note, String guardUserNotice) {
        this.mode = mode;
        this.source = source;
        this.note = note;
        this.guardUserNotice = guardUserNotice;
    }
}
