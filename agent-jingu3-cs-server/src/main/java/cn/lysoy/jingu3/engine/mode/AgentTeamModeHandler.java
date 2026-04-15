package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.config.Jingu3Properties;
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
 * <strong>指南 §7 Agent Team</strong>（八大行动模式之一，多角色驾驭）：<strong>Leader</strong> 模型先读用户诉求并拆出子任务描述；
 * <strong>专员</strong>（子 Agent）可跑多轮（{@code jingu3.engine.agent-team.max-specialist-rounds}），每轮在工具开启时复用
 * {@link AskModeHandler#runToolAugmentedOneShot} 与 Ask 相同的 JSON 工具路由；最后 <strong>Synthesize</strong> 一步把 Leader 子任务
 * 与专员轨迹拼成对用户单一答复。流式与阻塞路径步数语义对齐。
 */
@Slf4j
@Component
public class AgentTeamModeHandler implements ActionModeHandler {

    /** Leader / 专员直答 / 合成共用 */
    private final ChatLanguageModel chat;
    /** Leader 子任务文案、专员边界、合成模板等 */
    private final PromptAssembly prompts;
    /** 复用 Ask 工具管线，避免 AgentTeam 与 Ask 分叉两套 JSON 约定 */
    private final AskModeHandler askModeHandler;
    /** 读取工具总开关等 */
    private final Jingu3Properties jingu3Properties;
    /** 专员轮次数下限为 1，配置非法时在此 clamp */
    private final int maxSpecialistRounds;

    public AgentTeamModeHandler(
            ChatLanguageModel chat,
            PromptAssembly prompts,
            AskModeHandler askModeHandler,
            Jingu3Properties jingu3Properties,
            @Value("${jingu3.engine.agent-team.max-specialist-rounds:2}") int maxSpecialistRounds) {
        this.chat = chat;
        this.prompts = prompts;
        this.askModeHandler = askModeHandler;
        this.jingu3Properties = jingu3Properties;
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
        if (leader == null) {
            leader = "";
        }
        sink.stepBegin(1, "leader");
        sink.block(leader);
        sink.stepEnd(1);
        List<String> rounds = new ArrayList<>();
        StringBuilder acc = new StringBuilder();
        for (int r = 1; r <= maxSpecialistRounds; r++) {
            String out = runSpecialistRound(context, leader, r, acc.toString(), sink);
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
            String out = runSpecialistRound(context, leader, r, acc.toString(), null);
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

    /**
     * 专员一轮：工具关闭时用单段提示；工具开启时用 Ask 同款路由（提示词含 {@link cn.lysoy.jingu3.common.constant.PromptTemplates#AGENT_TEAM_SUB_BOUNDARY}）。
     *
     * @param sink 非空且本步调用了工具时下发 {@link StreamEventSink#toolResult}，供流式客户端展示
     */
    private String runSpecialistRound(
            ExecutionContext context, String leader, int round, String priorRoundsText, StreamEventSink sink) {
        if (!jingu3Properties.getTool().isEnabled()) {
            String fullPrompt =
                    round == 1
                            ? prompts.buildAgentTeamSubPrompt(context, leader)
                            : prompts.buildAgentTeamSubFollowUpPrompt(context, leader, priorRoundsText);
            String out = chat.generate(fullPrompt);
            return out == null ? "" : out;
        }
        String specialistUserParagraph =
                round == 1
                        ? prompts.buildAgentTeamSpecialistUserParagraphRound1(context, leader)
                        : prompts.buildAgentTeamSpecialistUserParagraphFollowUp(context, leader, priorRoundsText);
        AskModeHandler.AugmentedLlmAnswer ans =
                askModeHandler.runToolAugmentedOneShot(
                        true,
                        prompts.buildAgentTeamSpecialistToolRouterPrompt(context, specialistUserParagraph),
                        prompts.buildAgentTeamSpecialistDirectPrompt(context, specialistUserParagraph),
                        (toolId, toolOut) ->
                                prompts.buildAgentTeamSpecialistAfterToolPrompt(
                                        context, specialistUserParagraph, toolId, toolOut),
                        msg -> prompts.buildAgentTeamSpecialistToolFailurePrompt(context, specialistUserParagraph, msg));
        if (sink != null
                && ans.getInvokedToolId() != null
                && ans.getInvokedToolOutput() != null) {
            sink.toolResult(ans.getInvokedToolId(), ans.getInvokedToolOutput());
        }
        String t = ans.getText();
        return t == null ? "" : t;
    }
}
