package cn.lysoy.jingu3.engine.mode;

import cn.lysoy.jingu3.engine.ActionModeHandler;
import cn.lysoy.jingu3.engine.ExecutionContext;
import cn.lysoy.jingu3.prompt.PromptAssembly;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * ReAct 模式（v0.1 简化：单轮带 ReAct 风格系统提示，不接真实工具）。
 */
@Component
public class ReActModeHandler implements ActionModeHandler {

    private final ChatLanguageModel chat;
    private final PromptAssembly prompts;

    public ReActModeHandler(ChatLanguageModel chat, PromptAssembly prompts) {
        this.chat = chat;
        this.prompts = prompts;
    }

    @Override
    public String execute(ExecutionContext context) {
        String combined = prompts.buildReactCombinedPrompt(context.userMessage());
        return chat.generate(combined);
    }
}
