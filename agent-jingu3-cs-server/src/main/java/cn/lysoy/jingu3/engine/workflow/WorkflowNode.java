package cn.lysoy.jingu3.engine.workflow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 工作流图中的一个节点（指南 §6：预设流程节点）。
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkflowNode {

    private String id;
    /** 拼入 LLM 的节点级指令 */
    private String instruction;
}
