package cn.lysoy.jingu3.cron.dto;

import cn.lysoy.jingu3.cron.ScheduledTaskScope;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateScheduledTaskRequest {

    @NotNull
    private ScheduledTaskScope scope;

    /** {@link ScheduledTaskScope#CONVERSATION} 时必填 */
    private String conversationId;

    @NotNull
    private Instant nextRunAt;

    /** 预留：MVP 仅展示，不参与计算下次执行时间 */
    private String cronExpression;

    @NotNull
    @Valid
    private CronTaskPayloadDto payload;
}
