package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.team.AgentTeamResult;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.stream.StreamEventSink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 指南 §7 Agent Team：Leader 拆分子任务后，子 Agent 可配置多轮（{@code jingu3.engine.agent-team.max-specialist-rounds}），
 * 最后经一次 LLM 合成用户可见答复；流式与 {@link ChatLanguageModel#generate} 步数一致。
 */
@Slf4j
@Component
public class AgentTeamModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;
    private final int maxSpecialistRounds;

    public AgentTeamModeHandler(
            ChatLanguageModel chat,
            PromptAssembly prompts,
            @Value("${jingu3.engine.agent-team.max-specialist-rounds:2}") int maxSpecialistRounds) {
        this.chat = chat;
        this.prompts = prompts;
        this.maxSpecialistRounds = Math.max(1, maxSpecialistRounds);
    }

    @Override
    public String execute(ExecutionContext context) {
        AgentTeamResult result = runTeam(context);
        log.info(
                "agentTeam userId={} conv={} leaderChars={} rounds={}",
                context.getUserId(),
                context.getConversationId(),
                result.getLeaderSubtask().length(),
                result.getSpecialistRounds().size());
        return result.formatReply();
    }

    @Override
    public void stream(ExecutionContext context, StreamEventSink sink) {
        String leader = chat.generate(prompts.buildAgentTeamLeadPrompt(context));
        sink.stepBegin(1, "leader");
        sink.block(leader == null ? "" : leader);
        sink.stepEnd(1);
        List<String> rounds = new ArrayList<>();
        StringBuilder acc = new StringBuilder();
        for (int r = 1; r <= maxSpecialistRounds; r++) {
            String prompt =
                    r == 1
                            ? prompts.buildAgentTeamSubPrompt(context, leader)
                            : prompts.buildAgentTeamSubFollowUpPrompt(context, leader, acc.toString());
            String out = chat.generate(prompt);
            if (out == null) {
                out = "";
            }
            rounds.add(out);
            sink.stepBegin(1 + r, "specialist_" + r);
            sink.block(out);
            sink.stepEnd(1 + r);
            if (r < maxSpecialistRounds) {
                acc.append("第").append(r).append("轮：\n").append(out).append("\n\n");
            }
        }
        String trajectory = buildTrajectoryText(rounds);
        String synthesis =
                chat.generate(prompts.buildAgentTeamSynthesizePrompt(context, leader, trajectory));
        int synthStep = 1 + maxSpecialistRounds + 1;
        sink.stepBegin(synthStep, "synthesize");
        sink.block(synthesis == null ? "" : synthesis);
        sink.stepEnd(synthStep);
        log.info(
                "agentTeam stream userId={} conv={} leaderChars={} rounds={}",
                context.getUserId(),
                context.getConversationId(),
                leader.length(),
                rounds.size());
        sink.done();
    }

    private AgentTeamResult runTeam(ExecutionContext context) {
        String leader = chat.generate(prompts.buildAgentTeamLeadPrompt(context));
        if (leader == null) {
            leader = "";
        }
        List<String> rounds = new ArrayList<>();
        StringBuilder acc = new StringBuilder();
        for (int r = 1; r <= maxSpecialistRounds; r++) {
            String prompt =
                    r == 1
                            ? prompts.buildAgentTeamSubPrompt(context, leader)
                            : prompts.buildAgentTeamSubFollowUpPrompt(context, leader, acc.toString());
            String out = chat.generate(prompt);
            if (out == null) {
                out = "";
            }
            rounds.add(out);
            if (r < maxSpecialistRounds) {
                acc.append("第").append(r).append("轮：\n").append(out).append("\n\n");
            }
        }
        String trajectory = buildTrajectoryText(rounds);
        String synthesis =
                chat.generate(prompts.buildAgentTeamSynthesizePrompt(context, leader, trajectory));
        return new AgentTeamResult(leader, rounds, synthesis);
    }

    private static String buildTrajectoryText(List<String> rounds) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rounds.size(); i++) {
            sb.append("第").append(i + 1).append("轮：\n").append(rounds.get(i)).append("\n\n");
        }
        return sb.toString();
    }
}
