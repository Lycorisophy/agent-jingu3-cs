package cn.lysoy.jingu3.common.dto.dst;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatchDialogueStateRequest {

    @NotBlank
    private String stateJson;

    /** 可选；与库中 revision 不一致时返回 409（乐观锁占位） */
    private Long expectedRevision;
}
