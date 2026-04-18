package cn.lysoy.jingu3.service.context.stream;

/**
 * 流式事件类型枚举：与 {@link StreamEvent} 字段组合使用，前后端可据此分支渲染（打字机 / 分步卡片 / 错误提示）。
 */
public enum StreamEventType {
    /** 路由与用户信息，通常为首包 */
    META,
    /** 增量文本（Ask 流式） */
    TOKEN,
    /** 多步管线中某一步开始，携带 {@link StreamEvent#getLabel()} */
    STEP_BEGIN,
    /** 与 STEP_BEGIN 配对 */
    STEP_END,
    /** 阻塞式一整段输出（多步内某次 generate 的完整结果） */
    BLOCK,
    /** 工具执行完成（Ask/ReAct）；携带 {@link StreamEvent#getToolId()} 与 {@link StreamEvent#getToolOutput()} */
    TOOL_RESULT,
    /** 正常结束，之后不应再有业务事件 */
    DONE,
    /** 失败，携带 {@link StreamEvent#getError()} */
    ERROR
}
