package cn.lysoy.jingu3.engine.workflow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 工作流定义（由 classpath {@code workflows/*.json} 加载）。
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkflowDefinition {

    private String id;
    private List<WorkflowNode> nodes;
}
