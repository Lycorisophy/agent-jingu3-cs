package cn.lysoy.jingu3.common.vo.bpmn;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoricProcessInstanceItemVo {

    private String id;
    private String processDefinitionId;
    private String processDefinitionKey;
    private Long durationMs;
    private String endTime;
}
