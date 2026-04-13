package cn.lysoy.jingu3.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 写入 ES 事件文档（v0.6-C）；未填字段不落库。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateEventDocumentRequest {

    /** 不传则服务端生成 evt_ + UUID（无横线） */
    private String eventId;

    @NotBlank
    private String userId;

    private String conversationId;

    /** ISO-8601；不传则当前 UTC */
    private String timestamp;

    private String action;

    private String result;

    private List<String> actors;

    private String assertion;

    private String eventSubject;

    private String eventLocation;

    private List<String> triggerTerms;

    private String modality;

    private String temporalSemantic;

    private Map<String, Object> metadata;

    private String vectorId;

    private String messageId;
}
