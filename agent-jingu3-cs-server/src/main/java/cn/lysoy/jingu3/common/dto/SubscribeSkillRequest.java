package cn.lysoy.jingu3.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 订阅技能（写入 {@code user_skill}）。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscribeSkillRequest {

    @NotBlank
    private String skillId;
}
