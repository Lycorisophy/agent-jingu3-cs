package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.stream.StreamEventSink;
import cn.lysoy.jingu3.tool.ToolExecutionException;
import cn.lysoy.jingu3.tool.ToolRegistry;
import cn.lysoy.jingu3.tool.ToolRoutingParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 指南 §4 ReAct：迭代式「思考 → 行动 → 观察」；v0.3 起行动可解析为真实工具调用，观察为工具输出写入「已有过程」。
 */
@Component
public class ReActModeHandler implements ActionModeHandler {

    /** 与 PromptTemplates 中约定一致，用于显式声明本轮循环可结束 */
    private static final String TASK_COMPLETE = "[TASK_COMPLETE]";

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;
    private final Jingu3Properties properties;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final int maxSteps;

    public ReActModeHandler(
            ChatLanguageModel chat,
            PromptAssembly prompts,
            Jingu3Properties properties,
            ToolRegistry toolRegistry,
            ObjectMapper objectMapper,
            @Value("${jingu3.engine.react.max-steps:4}") int maxSteps) {
        this.chat = chat;
        this.prompts = prompts;
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.maxSteps = Math.max(1, maxSteps);
    }

    @Override
    public String execute(ExecutionContext context) {
        String userMessage = context.llmInput();
        StringBuilder display = new StringBuilder();
        StringBuilder prior = new StringBuilder();
        for (int step = 1; step <= maxSteps; step++) {
            String stepPrompt =
                    prompts.buildReactLoopStepPrompt(context, userMessage, prior.toString(), step, maxSteps);
            String out = chat.generate(stepPrompt);
            display.append("\n---\n第").append(step).append("步---\n").append(out);
            if (reactStepAfterGenerate(out, prior, step, null)) {
                break;
            }
        }
        return "【ReAct 轨迹】\n" + display;
    }

    /**
     * 处理单步模型输出：更新 prior；返回是否结束整个 ReAct 循环。
     *
     * @param sink 非 null 时在工具成功时发送 {@link StreamEventSink#toolResult}
     */
    private boolean reactStepAfterGenerate(
            String out, StringBuilder prior, int stepIndex, StreamEventSink sink) {
        boolean taskComplete = out != null && out.contains(TASK_COMPLETE);
        if (!properties.getTool().isEnabled()) {
            appendPriorSimple(prior, stepIndex, out);
            return taskComplete;
        }
        Optional<ToolRoutingParser.ReactFooterPayload> footer =
                ToolRoutingParser.parseReactFooter(out, objectMapper);
        if (footer.isEmpty()) {
            appendPriorSimple(prior, stepIndex, out);
            return taskComplete;
        }
        ToolRoutingParser.ReactFooterPayload f = footer.get();
        if ("done".equalsIgnoreCase(f.action())) {
            appendPriorSimple(prior, stepIndex, out);
            return true;
        }
        if ("invoke".equalsIgnoreCase(f.action())) {
            appendPriorSimple(prior, stepIndex, out);
            try {
                String obs = toolRegistry.execute(f.toolId(), f.input());
                if (sink != null) {
                    sink.toolResult(f.toolId(), obs);
                }
                prior.append("[系统观察] 工具 ")
                        .append(f.toolId())
                        .append(" 输出：\n")
                        .append(obs)
                        .append("\n");
            } catch (ToolExecutionException e) {
                prior.append("[系统观察] 工具错误：").append(e.getMessage()).append("\n");
            }
            return taskComplete;
        }
        appendPriorSimple(prior, stepIndex, out);
        return taskComplete;
    }

    private static void appendPriorSimple(StringBuilder prior, int stepIndex, String blockText) {
        prior.append("=== 第")
                .append(stepIndex)
                .append("步 ===\n")
                .append(blockText == null ? "" : blockText)
                .append("\n");
    }

    /**
     * 流式：每轮 LLM 仍阻塞 generate，将该步全文以 {@link StreamEventSink#block} 下发；工具成功时发
     * {@link StreamEventSink#toolResult}。
     */
    public void stream(ExecutionContext context, StreamEventSink sink) {
        String userMessage = context.llmInput();
        StringBuilder prior = new StringBuilder();
        for (int step = 1; step <= maxSteps; step++) {
            sink.stepBegin(step, "react_step_" + step);
            String stepPrompt =
                    prompts.buildReactLoopStepPrompt(context, userMessage, prior.toString(), step, maxSteps);
            String out = chat.generate(stepPrompt);
            sink.block(out == null ? "" : out);
            boolean stop = reactStepAfterGenerate(out, prior, step, sink);
            sink.stepEnd(step);
            if (stop) {
                break;
            }
        }
        sink.done();
    }
}
