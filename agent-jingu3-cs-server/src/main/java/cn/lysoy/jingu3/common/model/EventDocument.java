package cn.lysoy.jingu3.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 与 {@code jingu3-events} 索引字段对齐（见 classpath elasticsearch/jingu3-events-index.json）。
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventDocument {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("conversation_id")
    private String conversationId;

    @JsonProperty("user_id")
    private String userId;

    /** ISO-8601 或 ES date 可解析字符串 */
    private String timestamp;

    private String action;

    private String result;

    private List<String> actors;

    private String assertion;

    @JsonProperty("event_subject")
    private String eventSubject;

    @JsonProperty("event_location")
    private String eventLocation;

    @JsonProperty("trigger_terms")
    private List<String> triggerTerms;

    private String modality;

    @JsonProperty("temporal_semantic")
    private String temporalSemantic;

    private Map<String, Object> metadata;

    @JsonProperty("vector_id")
    private String vectorId;

    @JsonProperty("message_id")
    private String messageId;
}
