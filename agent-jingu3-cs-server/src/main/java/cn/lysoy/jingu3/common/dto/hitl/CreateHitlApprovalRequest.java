package cn.lysoy.jingu3.common.dto.hitl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateHitlApprovalRequest {

    @NotBlank
    private String conversationId;

    /** 可选：与某次 ReAct/Plan run 关联，便于续跑 */
    private String runId;

    @NotBlank
    private String payloadJson;
}
