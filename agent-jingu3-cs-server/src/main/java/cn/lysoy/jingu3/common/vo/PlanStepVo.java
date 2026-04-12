package cn.lysoy.jingu3.common.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 编排单步结果（与 {@link ChatVo#getPlanSteps()} 元素对应）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlanStepVo {

    private String mode;
    private String reply;
}
