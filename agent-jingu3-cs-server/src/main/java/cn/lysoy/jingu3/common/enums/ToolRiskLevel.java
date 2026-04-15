package cn.lysoy.jingu3.common.enums;

/**
 * 内置工具风险等级：供客户端与策略层展示；高危二次确认与 HITL 协同见路线图 v0.7。
 */
public enum ToolRiskLevel {

    /** 只读、纯计算等 */
    LOW,

    /** 写盘、改状态等 */
    MEDIUM,

    /** 任意代码执行等 */
    HIGH
}
