package cn.lysoy.jingu3.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 更新记忆条目（v0.6-B）；至少填写除 userId 外的一项可修改字段。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateMemoryEntryRequest {

    @NotBlank
    private String userId;

    private String summary;

    private String body;

    /** 若传入则更新 kind（FACT 或 EVENT） */
    private String kind;

    /** kind 为 FACT 时写入或清空标签；传空串可清空 */
    private String factTag;
}
