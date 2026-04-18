package cn.lysoy.jingu3.service.guard;

import cn.lysoy.jingu3.common.constant.EngineMessages;

import java.util.Locale;

/**
 * <strong>八大行动模式</strong>枚举，与 {@code docs/设计/AI智能体行动模式设计指南.md} 中 §1.3 / 各章模式一一对应；
 * 运行时由 {@link ModeRegistry} 映射到 {@link ActionModeHandler}，由 {@link cn.lysoy.jingu3.service.context.chat.ChatService} /
 * {@link cn.lysoy.jingu3.service.context.chat.ChatStreamService} 驱动，属<strong>驾驭工程</strong>核心契约。
 * <p>
 * 业界常见对应关系（便于与外部文档对照）：Ask≈单轮 QA；ReAct≈ Thought-Action-Observation 循环；
 * Plan-and-Execute≈先规划再按子任务执行；Workflow≈预设节点链；Agent Team≈多角色协作；
 * Cron/HITL/State 多对应运维、审批与状态机类场景。其中前五项为对话 API 可选；后三项主要通过引擎内或其它入口触发，
 * 见 {@link ActionModePolicy}。
 * </p>
 */
public enum ActionMode {
    /** 指南 §3 Ask：以单次（或带历史）生成回答为主；可与 Tool Registry 结合做工具选择与调用，流式语义见引擎配置。 */
    ASK,
    /**
     * 指南 §4 ReAct：推理与行动交织；经典表述为 Thought → Action → Observation 直至结束。
     * 实现为多步提示循环，观察以文本形式回注上下文。
     */
    REACT,
    /**
     * 指南 §5 Plan-and-Execute：先产出编号计划，再按子任务执行；失败时可重规划（Replanner，最小版为单次）。
     */
    PLAN_AND_EXECUTE,
    /**
     * 指南 §6 Workflow：按预定义节点顺序执行（本仓库为 JSON 顺序链）；无定义时回退两步固定提示。
     */
    WORKFLOW,
    /**
     * 指南 §7 Agent Team：Leader 拆分子任务 + 子角色执行；本仓库为两轮 LLM 与显式轨迹结构。
     */
    AGENT_TEAM,
    /** 指南 §8 Cron：定时意图说明；当前为演示文案，真实调度见路线图独立模块。 */
    CRON,
    /** 指南 §9 State Tracking：目标为跨轮次 DST；当前 Handler 仅为进程内计数占位，规范形态见指南 §9.0 与路线图。 */
    STATE_TRACKING,
    /** 指南 §10 Human-in-the-Loop：人在环待审批说明；队列与 API 见设计草案。 */
    HUMAN_IN_LOOP;

    /**
     * 解析客户端或分类器输出的模式名（大小写不敏感，支持连字符与下划线）。
     *
     * @param raw 如 {@code ASK}、{@code plan-and-execute}
     * @return 对应的 {@link ActionMode}
     * @throws IllegalArgumentException 当字符串为空或无法映射到任一枚举名时
     */
    public static ActionMode fromFlexibleName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(EngineMessages.MODE_CANNOT_BE_BLANK);
        }
        String n = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return ActionMode.valueOf(n);
    }
}
