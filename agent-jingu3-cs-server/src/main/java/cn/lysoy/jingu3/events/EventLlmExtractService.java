package cn.lysoy.jingu3.events;

import cn.lysoy.jingu3.events.model.EventExtractLlmResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 调用 LLM 从文本抽取事件与关系（仅 JSON，不落库）。
 */
@Slf4j
@Service
public class EventLlmExtractService {

    private static final String SYSTEM =
            "你是信息抽取助手。用户给一段文本，你输出且仅输出一个 JSON 对象，不要 Markdown 围栏。\n"
                    + "JSON 结构：\n"
                    + "{\"events\":[{\"conversationId\":string|null,\"eventTime\":string|null,"
                    + "\"action\":string,\"result\":string|null,\"actors\":string[]|null,\"assertion\":string|null,"
                    + "\"eventSubject\":string|null,\"eventLocation\":string|null,\"triggerTerms\":string[]|null,"
                    + "\"modality\":string|null,\"temporalSemantic\":string|null,\"metadata\":object|null,"
                    + "\"messageId\":string|null}],"
                    + "\"relations\":[{\"fromIndex\":number,\"toIndex\":number,\"relKind\":string,"
                    + "\"explanation\":string|null,\"confidence\":number|null}]}\n"
                    + "events 为文中抽取的事件列表（至少 0 条）。relations 中 fromIndex/toIndex 指向 events 数组下标（从 0 开始）。\n"
                    + "relKind 必须是以下之一：CAUSATION,EFFECT_CAUSE,TEMPORAL_BEFORE,TEMPORAL_AFTER,"
                    + "CONDITION,CONDITION_INVERSE,PURPOSE_MEANS,PURPOSE_GOAL,SUBEVENT,PARENT_EVENT,OTHER_RELATION。\n"
                    + "若 relKind 为 OTHER_RELATION，则 explanation 必填；否则可为 null。\n"
                    + "无关系时 relations 可为 []。";

    private final ChatLanguageModel chat;
    private final ObjectMapper objectMapper;

    public EventLlmExtractService(ChatLanguageModel chat, ObjectMapper objectMapper) {
        this.chat = chat;
        this.objectMapper = objectMapper;
    }

    public EventExtractLlmResult extract(String text) throws Exception {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text empty");
        }
        String raw = chat.generate(SYSTEM + "\n\n【文本】\n" + text);
        String json = stripJson(raw);
        return objectMapper.readValue(json, EventExtractLlmResult.class);
    }

    private static String stripJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1);
            }
            int end = s.lastIndexOf("```");
            if (end > 0) {
                s = s.substring(0, end);
            }
        }
        return s.trim();
    }
}
