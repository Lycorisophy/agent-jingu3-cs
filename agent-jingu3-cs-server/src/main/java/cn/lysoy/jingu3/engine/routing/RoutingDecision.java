package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.engine.ActionMode;
import lombok.Getter;

/**
 * 意图路由的<strong>不可变结果</strong>：本轮应采用的 {@link ActionMode}、决策来源（客户端 / 规则 / 模型 / 回落 / 守门），
 * 以及供日志、排障、产品提示用的说明文案。下游 {@link cn.lysoy.jingu3.service.chat.ChatStreamService} 等据此选 Handler 与提示词装配策略。
 */
@Getter
public class RoutingDecision {

    /** 本轮行动模式（八大模式之一，或经回落后的 ASK 等）。 */
    private final ActionMode mode;
    /** 该 {@link #mode} 由哪一层选出，用于可观测性与审计（优先级见 {@link IntentRouter}）。 */
    private final RoutingSource source;
    /** 人类可读补充说明，如规则命中关键词、模型分类理由、工作流 id 缺失回落等；可为空。 */
    private final String note;
    /**
     * 显式 Plan / Agent Team 经守门降为 Ask 时，可拼接进用户可见回复的前缀说明；其它情况为 {@code null}。
     * 与 {@link RoutingSource#EXPLICIT_GUARD} 配套，属于「驾驭工程」里对用户预期的显式对齐。
     */
    private final String guardUserNotice;

    /** 无守门用户提示时的便捷构造。 */
    public RoutingDecision(ActionMode mode, RoutingSource source, String note) {
        this(mode, source, note, null);
    }

    /** 完整构造；{@code guardUserNotice} 仅在显式重模式被降级时使用。 */
    public RoutingDecision(ActionMode mode, RoutingSource source, String note, String guardUserNotice) {
        this.mode = mode;
        this.source = source;
        this.note = note;
        this.guardUserNotice = guardUserNotice;
    }
}
