package cn.lysoy.jingu3.engine.routing;

import cn.lysoy.jingu3.common.constant.LogMessagePatterns;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.constant.PromptTemplates;
import cn.lysoy.jingu3.engine.ActionMode;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 使用主模型做轻量模式分类（第三源）；失败时返回 REACT 并记日志。
 */
@Slf4j
@Component
public class ModelIntentClassifier {

    private final ChatLanguageModel chat;

    public ModelIntentClassifier(ChatLanguageModel chat) {
        this.chat = chat;
    }

    public ActionMode classify(String userMessage) {
        String allowed = Arrays.stream(ActionMode.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String systemPart = PromptTemplates.INTENT_CLASSIFIER_SYSTEM_PREFIX + allowed;
        String prompt = systemPart
                + PromptFragments.PARAGRAPH_BREAK
                + PromptFragments.USER_INPUT_LABEL_WITH_NEWLINE
                + userMessage;
        try {
            String text = chat.generate(prompt).trim().toUpperCase(Locale.ROOT).replace('-', '_');
            try {
                return ActionMode.fromFlexibleName(text);
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
