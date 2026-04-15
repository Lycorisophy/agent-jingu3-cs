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
 * 指南 §3 Ask：单次对话为主；v0.3 起可经一轮 JSON 路由调用 {@link ToolRegistry} 内置工具再汇总。
 */
@Component
public class AskModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final StreamingChatLanguageModel streamingChat;
    private final PromptAssembly prompts;
    private final Jingu3Properties properties;
    private final ToolRegistry toolRegistry;
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
        String routeRaw = chat.generate(toolRouterPrompt);
        ToolRoutingParser.AskRoutePayload route =
                ToolRoutingParser.parseAskRoute(routeRaw, objectMapper)
                        .orElse(ToolRoutingParser.AskRoutePayload.direct());
        if (!route.useTool()) {
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
                        String msg = error.getMessage() != null ? error.getMessage() : error.toString();
                        sink.error(msg);
                    }
                });
    }
}
