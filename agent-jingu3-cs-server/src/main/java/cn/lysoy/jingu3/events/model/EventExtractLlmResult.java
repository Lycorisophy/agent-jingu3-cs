package cn.lysoy.jingu3.events.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * LLM 抽取 JSON 反序列化（与 {@link cn.lysoy.jingu3.events.EventLlmExtractService} 提示词约定一致）。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventExtractLlmResult {

    private List<EventDraft> events;
    private List<RelationDraft> relations;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventDraft {
        private String conversationId;
        private String eventTime;
        private String action;
        private String result;
        private List<String> actors;
        private String assertion;
        private String eventSubject;
        private String eventLocation;
        private List<String> triggerTerms;
        private String modality;
        private String temporalSemantic;
        private Object metadata;
        private String messageId;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RelationDraft {
        private int fromIndex;
        private int toIndex;
        private String relKind;
        private String explanation;
        private Double confidence;
    }
}
