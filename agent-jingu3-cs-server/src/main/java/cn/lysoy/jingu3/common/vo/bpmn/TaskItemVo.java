package cn.lysoy.jingu3.common.vo.bpmn;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskItemVo {

    private String id;
    private String name;
    private String processInstanceId;
    private String assignee;
}
