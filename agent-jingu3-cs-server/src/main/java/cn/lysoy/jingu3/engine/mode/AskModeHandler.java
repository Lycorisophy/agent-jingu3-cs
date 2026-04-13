package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import cn.lysoy.jingu3.stream.StreamEventSink;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 指南 §3 Ask：以单次对话生成为主，侧重直接回答；业界常作为「无工具 / 轻工具」基线。
 * <p>
 * 同步路径：{@link ChatLanguageModel#generate(String)} 一次返回全文。<br>
 * 流式路径：{@link StreamingChatLanguageModel#generate} + {@link StreamingResponseHandler#onNext}，
 * 将 Ollama 侧 token/分片经 {@link StreamEventSink#token} 推给前端（打字机）。
 * </p>
 * <p>工具路由与 Tool Registry 为路线图能力，当前未接入。</p>
 */
@Component
public class AskModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final StreamingChatLanguageModel streamingChat;
    private final PromptAssembly prompts;

    public AskModeHandler(
            ChatLanguageModel chat,
            StreamingChatLanguageModel streamingChat,
            PromptAssembly prompts) {
        this.chat = chat;
        this.streamingChat = streamingChat;
        this.prompts = prompts;
    }

    /**
     * 阻塞式单轮问答：提示词 = 系统角色 + 用户段落（见 {@link PromptAssembly#buildAskCombinedPrompt}）。
     */
    @Override
    public String execute(ExecutionContext context) {
        String combined = prompts.buildAskCombinedPrompt(context);
        return chat.generate(combined);
    }

    /**
     * 流式单轮：与 {@link #execute} 使用同一拼接提示；结束时在 {@link StreamingResponseHandler#onComplete} 中
     * 调用 {@link StreamEventSink#done()}，错误走 {@link StreamEventSink#error}。
     *
     * @param context 本轮上下文
     * @param sink    事件出口（SSE 或 WebSocket 适配）
     */
    public void stream(ExecutionContext context, StreamEventSink sink) {
        String combined = prompts.buildAskCombinedPrompt(context);
        // LangChain4j：单条 UserMessage 承载「系统+用户」合并文案，与阻塞 API 语义对齐
        streamingChat.generate(
                List.of(UserMessage.from(combined)),
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
