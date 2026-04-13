package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.engine.team.AgentTeamResult;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.stream.StreamEventSink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 指南 §7 Agent Team（中心化 Leader 变体）：第一轮由「主 Agent」根据用户输入拆出可执行的子任务描述，
 * 第二轮由「子 Agent」仅针对该子任务产出答案。业界多智能体系统中常配合消息总线与共享状态；
 * 本仓库仍以单进程、两次 LLM 调用模拟，轨迹由 {@link AgentTeamResult} 承载。
 * <p>流式：Leader / Specialist 各一步 BLOCK。</p>
 */
@Slf4j
@Component
public class AgentTeamModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;

    public AgentTeamModeHandler(ChatLanguageModel chat, PromptAssembly prompts) {
        this.chat = chat;
        this.prompts = prompts;
    }

    /**
     * 两轮阻塞调用：Leader → Specialist，返回合并文案。
     */
    @Override
    public String execute(ExecutionContext context) {
        String subtask = chat.generate(prompts.buildAgentTeamLeadPrompt(context));
        String specialist = chat.generate(prompts.buildAgentTeamSubPrompt(context, subtask));
        AgentTeamResult result = new AgentTeamResult(subtask, specialist);
        log.info("agentTeam userId={} conv={} leaderChars={} specialistChars={}",
                context.getUserId(),
                context.getConversationId(),
                subtask.length(),
                specialist.length());
        return result.formatReply();
    }

    /**
     * 流式：两步各发 BLOCK，步标签为 {@code leader} / {@code specialist}。
     */
    public void stream(ExecutionContext context, StreamEventSink sink) {
        sink.stepBegin(1, "leader");
        String subtask = chat.generate(prompts.buildAgentTeamLeadPrompt(context));
        sink.block(subtask == null ? "" : subtask);
        sink.stepEnd(1);
        sink.stepBegin(2, "specialist");
        String specialist = chat.generate(prompts.buildAgentTeamSubPrompt(context, subtask));
        sink.block(specialist == null ? "" : specialist);
        sink.stepEnd(2);
        log.info("agentTeam stream userId={} conv={} leaderChars={} specialistChars={}",
                context.getUserId(),
                context.getConversationId(),
                subtask.length(),
                specialist.length());
        sink.done();
    }
}
