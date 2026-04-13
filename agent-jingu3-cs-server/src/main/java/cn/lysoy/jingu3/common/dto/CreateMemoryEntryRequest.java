package cn.lysoy.jingu3.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 创建记忆条目（v0.6 M1 实验 API）。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateMemoryEntryRequest {

    @NotBlank
    private String userId;

    /** FACT 或 EVENT */
    @NotBlank
    private String kind;

    private String summary;

    private String body;

    /** kind=FACT 时可选，写入 fact_metadata.tag */
    private String factTag;
}
