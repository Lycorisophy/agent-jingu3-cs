package cn.lysoy.jingu3.engine.team;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import lombok.Getter;

/**
 * Agent Team 一轮协作的可观测结果：封装「主 Agent 给出的子任务描述」与「子 Agent 针对该子任务的输出」，
 * 与指南 §7 中 Leader / Specialist 分工一致，便于日志与前端分块展示。
 */
@Getter
public class AgentTeamResult {

    /** Leader 阶段产出、作为 Specialist 的输入摘要 */
    private final String leaderSubtask;
    /** Specialist 阶段最终对用户可见片段（仍由单次 LLM 生成） */
    private final String specialistOutput;

    public AgentTeamResult(String leaderSubtask, String specialistOutput) {
        this.leaderSubtask = leaderSubtask;
        this.specialistOutput = specialistOutput;
    }

    /** 与同步 {@link cn.lysoy.jingu3.engine.mode.AgentTeamModeHandler#execute} 返回字符串格式一致。 */
    public String formatReply() {
        return "【主 Agent 子任务】\n"
                + leaderSubtask
                + PromptFragments.PARAGRAPH_BREAK
                + "【子 Agent 输出】\n"
                + specialistOutput;
    }
}
