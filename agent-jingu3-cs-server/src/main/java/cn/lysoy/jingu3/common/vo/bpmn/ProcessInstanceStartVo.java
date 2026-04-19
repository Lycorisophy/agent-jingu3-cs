package cn.lysoy.jingu3.common.vo.bpmn;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ProcessInstanceStartVo {

    private String processInstanceId;
    private String processDefinitionId;
    private Map<String, Object> variables = new HashMap<>();
}
