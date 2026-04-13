package cn.lysoy.jingu3.dst.dto;

import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.dst.entity.DialogueStateEntity;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class DialogueStateVo {

    String conversationId;
    String schemaVersion;
    String stateJson;
    long revision;
    Instant updatedAt;

    public static DialogueStateVo from(DialogueStateEntity e) {
        return DialogueStateVo.builder()
                .conversationId(e.getConversationId())
                .schemaVersion(e.getSchemaVersion())
                .stateJson(e.getStateJson())
                .revision(e.getRevision())
                .updatedAt(UtcTime.toInstant(e.getUpdatedAt()))
                .build();
    }
}
