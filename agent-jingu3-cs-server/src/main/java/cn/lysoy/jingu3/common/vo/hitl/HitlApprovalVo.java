package cn.lysoy.jingu3.common.vo.hitl;

import cn.lysoy.jingu3.util.UtcTime;
import cn.lysoy.jingu3.common.enums.HitlApprovalStatus;
import cn.lysoy.jingu3.common.entity.HitlApprovalEntity;
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

    public static HitlApprovalVo from(HitlApprovalEntity e) {
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
