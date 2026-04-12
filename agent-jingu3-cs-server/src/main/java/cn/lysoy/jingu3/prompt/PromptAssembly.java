package cn.lysoy.jingu3.prompt;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.constant.PromptTemplates;
import org.springframework.stereotype.Component;

/**
 * 提示词拼装（ENG:提示词工程）；模板取自 {@link PromptTemplates}，结构片段取自 {@link PromptFragments}。
 */
@Component
public class PromptAssembly {

    /**
     * Ask 系统提示（与 {@link PromptTemplates#ASK_SYSTEM} 一致，便于按模式取文案）。
     */
    public String askSystemPrompt() {
        return PromptTemplates.ASK_SYSTEM;
    }

    /**
     * ReAct 系统提示。
     */
    public String reactSystemPrompt() {
        return PromptTemplates.REACT_SYSTEM;
    }

    /**
     * Ask：系统提示 + 用户段落，供单次 generate。
     */
    public String buildAskCombinedPrompt(String userMessage) {
        return PromptTemplates.ASK_SYSTEM
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + userMessage;
    }

    /**
     * ReAct：系统提示 + 用户段落。
     */
    public String buildReactCombinedPrompt(String userMessage) {
        return PromptTemplates.REACT_SYSTEM
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_LABEL_WITH_NEWLINE
                + userMessage;
    }
}
