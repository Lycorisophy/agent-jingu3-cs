package cn.lysoy.jingu3.hitl.dto;

import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.hitl.HitlApprovalStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class HitlApprovalVo {

    Long id;
    String conversationId;
    String runId;
    HitlApprovalStatus status;
    String payloadJson;
    Instant createdAt;
    Instant resolvedAt;
    String resolverUserId;

    public static HitlApprovalVo from(cn.lysoy.jingu3.hitl.entity.HitlApprovalEntity e) {
        return HitlApprovalVo.builder()
                .id(e.getId())
                .conversationId(e.getConversationId())
                .runId(e.getRunId())
                .status(e.getStatus())
                .payloadJson(e.getPayloadJson())
                .createdAt(UtcTime.toInstant(e.getCreatedAt()))
                .resolvedAt(UtcTime.toInstant(e.getResolvedAt()))
                .resolverUserId(e.getResolverUserId())
                .build();
    }
}
