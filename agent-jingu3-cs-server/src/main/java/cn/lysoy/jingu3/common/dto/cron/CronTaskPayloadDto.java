package cn.lysoy.jingu3.common.dto.cron;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.job.ScheduledTaskScope;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 到期时反序列化为 {@link ChatRequest} 子集并触发对话。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CronTaskPayloadDto {

    @NotBlank
    private String message;

    private String mode;
    private List<String> modePlan;
    private String workflowId;

    /** 仅 {@link ScheduledTaskScope#GLOBAL} 时可选，用于指定会话 */
    private String conversationId;
}
