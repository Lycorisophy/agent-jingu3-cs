package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.stream.StreamEventSink;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 指南 §4 ReAct（Reasoning + Acting）：迭代式「思考 → 行动 → 观察」直至任务可结束。
 * 经典论文与 LangChain 等实现中，观察常来自工具；本仓库 MVP 将「观察」写进提示中的「已有过程」，
 * 以纯文本模拟回流，后续可替换为真实工具输出。
 * <p>
 * 终止：模型在任一步输出包含 {@link #TASK_COMPLETE} 时提前结束，或达到 {@code jingu3.engine.react.max-steps}。
 * </p>
 */
@Component
public class ReActModeHandler implements ActionModeHandler {

    /** 与 PromptTemplates 中约定一致，用于显式声明本轮循环可结束 */
    private static final String TASK_COMPLETE = "[TASK_COMPLETE]";

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;
    private final int maxSteps;

    public ReActModeHandler(
            ChatLanguageModel chat,
            PromptAssembly prompts,
            @Value("${jingu3.engine.react.max-steps:4}") int maxSteps) {
        this.chat = chat;
        this.prompts = prompts;
        this.maxSteps = Math.max(1, maxSteps);
    }

    @Override
    public String execute(ExecutionContext context) {
        String userMessage = context.llmInput();
        StringBuilder trace = new StringBuilder();
        for (int step = 1; step <= maxSteps; step++) {
            String prior = trace.toString();
            String stepPrompt = prompts.buildReactLoopStepPrompt(userMessage, prior, step, maxSteps);
            String out = chat.generate(stepPrompt);
            trace.append("\n---\n第").append(step).append("步---\n").append(out);
            if (out != null && out.contains(TASK_COMPLETE)) {
                break;
            }
        }
        return "【ReAct 轨迹】\n" + trace;
    }

    /**
     * 流式：每轮 LLM 仍阻塞 generate，将该步全文以 {@link StreamEventSink#block} 下发，并带 STEP 边界便于 UI 分节。
     */
    public void stream(ExecutionContext context, StreamEventSink sink) {
        String userMessage = context.llmInput();
        StringBuilder trace = new StringBuilder();
        for (int step = 1; step <= maxSteps; step++) {
            sink.stepBegin(step, "react_step_" + step);
            String prior = trace.toString();
            String stepPrompt = prompts.buildReactLoopStepPrompt(userMessage, prior, step, maxSteps);
            String out = chat.generate(stepPrompt);
            trace.append("\n---\n第").append(step).append("步---\n").append(out);
            sink.block(out == null ? "" : out);
            sink.stepEnd(step);
            if (out != null && out.contains(TASK_COMPLETE)) {
                break;
            }
        }
        sink.done();
    }
}
