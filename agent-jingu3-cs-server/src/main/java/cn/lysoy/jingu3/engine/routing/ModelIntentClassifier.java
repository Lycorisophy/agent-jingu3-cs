package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.common.constant.LogMessagePatterns;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.constant.PromptTemplates;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ActionModePolicy;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 指南 §2 中「模型辅助选型」的实现：当既无显式 {@code mode} 又未命中规则路由时，用一次短 LLM 调用输出枚举名。
 * 非流式、非用户可见回复，故仍用阻塞 {@link ChatLanguageModel}。
 * <p>仅允许 {@link ActionModePolicy} 中的对话可选模式；其它解析结果降级为 REACT 并记日志。</p>
 */
@Slf4j
@Component
public class ModelIntentClassifier {

    private final ChatLanguageModel chat;

    public ModelIntentClassifier(ChatLanguageModel chat) {
        this.chat = chat;
    }

    public ActionMode classify(String userMessage) {
        String allowed = ActionModePolicy.conversationSelectableNamesJoined();
        String systemPart = PromptTemplates.INTENT_CLASSIFIER_SYSTEM_PREFIX + allowed;
        String prompt = systemPart
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_INPUT_LABEL_WITH_NEWLINE
                + userMessage;
        try {
            String text = chat.generate(prompt).trim().toUpperCase(Locale.ROOT).replace('-', '_');
            try {
                ActionMode mode = ActionMode.fromFlexibleName(text);
                if (!ActionModePolicy.isConversationSelectable(mode)) {
                    log.warn(LogMessagePatterns.INTENT_CLASSIFIER_UNPARSEABLE_OUTPUT, text);
                    return ActionMode.REACT;
                }
                return mode;
            } catch (IllegalArgumentException ex) {
                log.warn(LogMessagePatterns.INTENT_CLASSIFIER_UNPARSEABLE_OUTPUT, text);
                return ActionMode.REACT;
            }
        } catch (Exception e) {
            log.warn(LogMessagePatterns.INTENT_CLASSIFIER_FAILED, e.toString());
            return ActionMode.REACT;
        }
    }
}
