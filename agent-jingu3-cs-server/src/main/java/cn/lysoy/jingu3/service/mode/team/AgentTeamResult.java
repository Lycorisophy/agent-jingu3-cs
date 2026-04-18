package cn.lysoy.jingu3.service.mode.team;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agent Team 协作结果：主 Agent 子任务、子 Agent 多轮输出与可选合成答复（v0.5 边界内多轮 + 合成）。
 */
@Getter
public class AgentTeamResult {

    private final String leaderSubtask;
    private final List<String> specialistRounds;
    private final String synthesizedReply;

    public AgentTeamResult(String leaderSubtask, String specialistOutput) {
        this(leaderSubtask, Collections.singletonList(specialistOutput == null ? "" : specialistOutput), null);
    }

    public AgentTeamResult(String leaderSubtask, List<String> specialistRounds, String synthesizedReply) {
        this.leaderSubtask = leaderSubtask == null ? "" : leaderSubtask;
        this.specialistRounds = specialistRounds == null
                ? List.of()
                : List.copyOf(new ArrayList<>(specialistRounds));
        this.synthesizedReply = synthesizedReply;
    }

    /** 最后一轮子 Agent 输出；无则空串。 */
    public String getSpecialistOutput() {
        return specialistRounds.isEmpty() ? "" : specialistRounds.get(specialistRounds.size() - 1);
    }

    /**
     * 与同步 {@link cn.lysoy.jingu3.service.mode.handler.AgentTeamModeHandler#execute} 返回字符串格式一致；
     * 含合成段（若有）、主 Agent 子任务、各轮子 Agent 轨迹。
     */
    public String formatReply() {
        StringBuilder sb = new StringBuilder();
        if (synthesizedReply != null && !synthesizedReply.isBlank()) {
            sb.append("【合成答复】\n")
                    .append(synthesizedReply)
                    .append(PromptFragments.PARAGRAPH_BREAK);
        }
        sb.append("【主 Agent 子任务】\n")
                .append(leaderSubtask)
                .append(PromptFragments.PARAGRAPH_BREAK);
        if (specialistRounds.size() == 1) {
            sb.append("【子 Agent 输出】\n").append(specialistRounds.get(0));
        } else {
            for (int i = 0; i < specialistRounds.size(); i++) {
                sb.append("【子 Agent 第")
                        .append(i + 1)
                        .append("轮】\n")
                        .append(specialistRounds.get(i));
                if (i < specialistRounds.size() - 1) {
                    sb.append(PromptFragments.PARAGRAPH_BREAK);
                }
            }
        }
        return sb.toString();
    }
}
