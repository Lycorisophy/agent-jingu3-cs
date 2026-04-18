package cn.lysoy.jingu3.service.guard.routing;

import cn.lysoy.jingu3.common.constant.LogMessagePatterns;
import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.constant.PromptTemplates;
import cn.lysoy.jingu3.service.guard.ActionMode;
import cn.lysoy.jingu3.service.guard.ActionModePolicy;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * 指南 §2 中「模型辅助选型」的实现：当既无显式 {@code mode} 又未命中规则路由时，用一次短 LLM 调用输出枚举名。
 * 非流式、非用户可见回复，故仍用阻塞 {@link ChatLanguageModel}。
 * <p>仅允许 {@link ActionModePolicy} 中的对话可选模式；其它解析结果或调用失败时降级为 ASK 并记日志。</p>
 * <p>{@link #classifyOptional(String)} 在 LLM 调用异常时返回 {@link Optional#empty()}，供显式模式守门在失败时尊重用户选择。</p>
 */
@Slf4j
@Component
public class ModelIntentClassifier {

    /** 专用于短分类提示的阻塞模型（与主对话模型可为同一 Bean，但提示极短） */
    private final ChatLanguageModel chat;

    public ModelIntentClassifier(ChatLanguageModel chat) {
        this.chat = chat;
    }

    /**
     * 与 {@link #classify(String)} 相同解析规则；若底层 LLM 调用抛错则返回 empty（不当作 ASK）。
     */
    public Optional<ActionMode> classifyOptional(String userMessage) {
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
                    return Optional.of(ActionMode.ASK);
                }
                return Optional.of(mode);
            } catch (IllegalArgumentException ex) {
                log.warn(LogMessagePatterns.INTENT_CLASSIFIER_UNPARSEABLE_OUTPUT, text);
                return Optional.of(ActionMode.ASK);
            }
        } catch (Exception e) {
            log.warn(LogMessagePatterns.INTENT_CLASSIFIER_FAILED, e.toString());
            return Optional.empty();
        }
    }

    public ActionMode classify(String userMessage) {
        return classifyOptional(userMessage).orElse(ActionMode.ASK);
    }
}
