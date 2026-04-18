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

    /**
     * kind=FACT 时可选：{@link cn.lysoy.jingu3.rag.FactTemporalTier} 名（PERMANENT / LONG_TERM / SHORT_TERM），默认
     * SHORT_TERM。
     */
    private String temporalTier;

    /**
     * kind=FACT 时可选：为 true 则创建时写入 {@code fact_metadata.confirmed_at}（当前 UTC 时刻）。
     */
    private Boolean confirmed;
}
