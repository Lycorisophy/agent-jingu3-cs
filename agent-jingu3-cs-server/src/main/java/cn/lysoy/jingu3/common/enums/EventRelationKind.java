package cn.lysoy.jingu3.common.enums;

/**
 * 有向事件关系 A → B（与 docs/设计/事件模型与关系类型.md 对齐）。
 */
public enum EventRelationKind {

    CAUSATION,
    EFFECT_CAUSE,
    TEMPORAL_BEFORE,
    TEMPORAL_AFTER,
    CONDITION,
    CONDITION_INVERSE,
    PURPOSE_MEANS,
    PURPOSE_GOAL,
    SUBEVENT,
    PARENT_EVENT,
    OTHER_RELATION
}
