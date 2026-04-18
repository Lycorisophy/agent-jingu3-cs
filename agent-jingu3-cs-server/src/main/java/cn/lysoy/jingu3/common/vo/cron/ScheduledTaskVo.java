package cn.lysoy.jingu3.common.vo.cron;

import cn.lysoy.jingu3.job.ScheduledTaskScope;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ScheduledTaskVo {

    Long id;
    String ownerUserId;
    ScheduledTaskScope scope;
    String conversationId;
    String cronExpression;
    Instant nextRunAt;
    String payloadJson;
    boolean enabled;
    Instant lastRunAt;
    String lastStatus;
    String lastError;
    Instant createdAt;
    Instant updatedAt;
}
