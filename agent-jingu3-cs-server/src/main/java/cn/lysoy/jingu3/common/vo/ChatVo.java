package cn.lysoy.jingu3.common.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 聊天接口出参（VO），与 {@link ChatRequest} 对应。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatVo {

    private String userId;
    private String username;
    private String reply;
    private String actionMode;
    private String routingSource;
    /** 非空表示本次为 modePlan 编排；单模式时为 {@code null} */
    private List<PlanStepVo> planSteps;
}
