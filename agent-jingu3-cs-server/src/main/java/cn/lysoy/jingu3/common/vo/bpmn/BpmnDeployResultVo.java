package cn.lysoy.jingu3.common.vo.bpmn;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class BpmnDeployResultVo {

    private String deploymentId;
    private String deploymentName;
    private List<ProcessDefinitionItemVo> definitions = new ArrayList<>();
}
