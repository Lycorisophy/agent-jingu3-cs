package cn.lysoy.jingu3.service.mode.handler;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.mode.ActionModeHandler;
import cn.lysoy.jingu3.service.guard.ExecutionContext;
import cn.lysoy.jingu3.service.prompt.PromptAssembly;
import cn.lysoy.jingu3.service.context.stream.StreamErrorMessages;
import cn.lysoy.jingu3.service.context.stream.StreamEventSink;
import cn.lysoy.jingu3.skill.tool.ToolExecutionException;
import cn.lysoy.jingu3.skill.tool.ToolRegistry;
import cn.lysoy.jingu3.skill.tool.ToolRoutingParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <strong>指南 §3 Ask 模式</strong>（八大行动模式之一）：以单次（或带历史）模型生成为主；在
 * {@code jingu3.tool.enabled=true} 时走「<strong>工具路由 JSON → 可选一次工具 → 再生成用户可见答复</strong>」闭环，
 * 工具调用经 {@link ToolRegistry}，路由格式由 {@link ToolRoutingParser} 解析。
 * <p>{@link #stream} 使用流式模型 API 输出 TOKEN，并在工具路径上发出 {@link cn.lysoy.jingu3.service.context.stream.StreamEventSink#toolResult}。</p>
 */
@Component
public class AskModeHandler implements ActionModeHandler {

    /** 阻塞式：工具路由、再生成、Agent Team 专员轮等复用 */
    private final ChatLanguageModel chat;
    /** 流式：最终对用户的打字机输出 */
    private final StreamingChatLanguageModel streamingChat;
    /** 提示词工程：Ask 各阶段模板拼装 */
    private final PromptAssembly prompts;
    /** 工具总开关等 */
    private final Jingu3Properties properties;
    /** 内置工具执行入口 */
    private final ToolRegistry toolRegistry;
    /** 与模型约定 JSON 解析 */
    private final ObjectMapper objectMapper;

    public AskModeHandler(
            ChatLanguageModel chat,
            StreamingChatLanguageModel streamingChat,
            PromptAssembly prompts,
            Jingu3Properties properties,
            ToolRegistry toolRegistry,
            ObjectMapper objectMapper) {
        this.chat = chat;
        this.streamingChat = streamingChat;
        this.prompts = prompts;
        this.properties = properties;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public String execute(ExecutionContext context) {
        // 统一管线：与 Agent Team 专员等共享 runToolAugmentedOneShot，避免分叉逻辑
        return runToolAugmentedOneShot(
                        properties.getTool().isEnabled(),
                        prompts.buildAskToolRouterPrompt(context),
                        prompts.buildAskCombinedPrompt(context),
                        (toolId, toolOut) -> prompts.buildAskAfterToolPrompt(context, toolId, toolOut),
                        msg -> prompts.buildAskToolFailurePrompt(context, msg))
                .getText();
    }

    /**
     * 与 {@link #execute} 相同的「JSON 路由 → 可选工具 → 最终 generate」管线，提示词由调用方传入（如 Agent Team 专员轮）。
     *
     * @param afterToolPromptBuilder 参数为 toolId、工具原始输出，返回送入模型的完整提示词
     */
    public AugmentedLlmAnswer runToolAugmentedOneShot(
            boolean toolsEnabled,
            String toolRouterPrompt,
            String directAnswerPrompt,
            BiFunction<String, String, String> afterToolPromptBuilder,
            Function<String, String> toolFailurePromptBuilder) {
        if (!toolsEnabled) {
            return new AugmentedLlmAnswer(chat.generate(directAnswerPrompt), null, null);
        }
        // 第一步：模型仅输出一行 JSON，声明 direct 或 tool(toolId,input)
        String routeRaw = chat.generate(toolRouterPrompt);
        ToolRoutingParser.AskRoutePayload route =
                ToolRoutingParser.parseAskRoute(routeRaw, objectMapper)
                        .orElse(ToolRoutingParser.AskRoutePayload.direct());
        if (!route.useTool()) {
            // 解析失败或显式 direct：退回单次问答提示
            return new AugmentedLlmAnswer(chat.generate(directAnswerPrompt), null, null);
        }
        try {
            String toolOut = toolRegistry.execute(route.toolId(), route.input());
            String text = chat.generate(afterToolPromptBuilder.apply(route.toolId(), toolOut));
            return new AugmentedLlmAnswer(text, route.toolId(), toolOut);
        } catch (ToolExecutionException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            String text = chat.generate(toolFailurePromptBuilder.apply(msg));
            return new AugmentedLlmAnswer(text, route.toolId(), null);
        }
    }

    /** {@link #runToolAugmentedOneShot} 的生成结果；工具未调用时 {@link #getInvokedToolId()} 为 null。 */
    public static final class AugmentedLlmAnswer {
        private final String text;
        private final String invokedToolId;
        private final String invokedToolOutput;

        public AugmentedLlmAnswer(String text, String invokedToolId, String invokedToolOutput) {
            this.text = text;
            this.invokedToolId = invokedToolId;
            this.invokedToolOutput = invokedToolOutput;
        }

        public String getText() {
            return text;
        }

        public String getInvokedToolId() {
            return invokedToolId;
        }

        public String getInvokedToolOutput() {
            return invokedToolOutput;
        }
    }

    @Override
    public void stream(ExecutionContext context, StreamEventSink sink) {
        if (!properties.getTool().isEnabled()) {
            streamDirectOnly(context, sink);
            return;
        }
        String routeRaw = chat.generate(prompts.buildAskToolRouterPrompt(context));
        ToolRoutingParser.AskRoutePayload route =
                ToolRoutingParser.parseAskRoute(routeRaw, objectMapper)
                        .orElse(ToolRoutingParser.AskRoutePayload.direct());
        if (!route.useTool()) {
            streamDirectOnly(context, sink);
            return;
        }
        try {
            String toolOut = toolRegistry.execute(route.toolId(), route.input());
            sink.toolResult(route.toolId(), toolOut);
            streamCombined(prompts.buildAskAfterToolPrompt(context, route.toolId(), toolOut), sink);
        } catch (ToolExecutionException e) {
            streamCombined(prompts.buildAskToolFailurePrompt(context, e.getMessage()), sink);
        }
    }

    private void streamDirectOnly(ExecutionContext context, StreamEventSink sink) {
        streamCombined(prompts.buildAskCombinedPrompt(context), sink);
    }

    private void streamCombined(String combinedPrompt, StreamEventSink sink) {
        streamingChat.generate(
                List.of(UserMessage.from(combinedPrompt)),
                new StreamingResponseHandler() {
                    @Override
                    public void onNext(String token) {
                        sink.token(token);
                    }

                    @Override
                    public void onComplete(Response response) {
                        sink.done();
                    }

                    @Override
                    public void onError(Throwable error) {
                        sink.error(StreamErrorMessages.fromThrowable(error));
                    }
                });
    }
}
