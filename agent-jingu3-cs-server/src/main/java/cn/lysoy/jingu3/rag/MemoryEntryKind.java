package cn.lysoy.jingu3.rag;

/**
 * 记忆条目的<strong>语义分类</strong>（记忆与知识工程）：写入 Milvus / 业务表时区分「稳定事实」与「时序事件」，
 * 便于检索加权、过期策略与后续与事件溯源模型对齐（当前为 M1 草案枚举）。
 */
public enum MemoryEntryKind {
    /** 相对稳定、可反复引用的陈述性知识（如用户偏好、长期设定）。 */
    FACT,
    /** 带时间语境的一次性记录（如某次对话结论、某步操作结果），检索时可与 FACT 分路或降权。 */
    EVENT
}
