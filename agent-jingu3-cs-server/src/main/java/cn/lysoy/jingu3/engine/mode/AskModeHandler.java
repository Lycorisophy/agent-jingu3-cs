package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * 传统对话模式（Ask）：单次生成。
 */
@Component
public class AskModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;

    public AskModeHandler(ChatLanguageModel chat, PromptAssembly prompts) {
        this.chat = chat;
        this.prompts = prompts;
    }

    @Override
    public String execute(ExecutionContext context) {
        String combined = prompts.buildAskCombinedPrompt(context.userMessage());
        return chat.generate(combined);
    }
}
