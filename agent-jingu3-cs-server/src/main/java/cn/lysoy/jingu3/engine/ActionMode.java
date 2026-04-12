package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.common.constant.EngineMessages;

/**
 * 八大行动模式（与 docs/设计/AI智能体行动模式设计指南 对齐）。
 */
public enum ActionMode {
    ASK,
    REACT,
    PLAN_AND_EXECUTE,
    WORKFLOW,
    AGENT_TEAM,
    CRON,
    STATE_TRACKING,
    HUMAN_IN_LOOP;

    /**
     * 解析客户端或分类器输出的模式名（大小写不敏感，支持连字符与下划线）。
     */
    public static ActionMode fromFlexibleName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(EngineMessages.MODE_CANNOT_BE_BLANK);
        }
        String n = raw.trim().toUpperCase().replace('-', '_');
        return ActionMode.valueOf(n);
    }
}
