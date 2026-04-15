package cn.lysoy.jingu3.engine.workflow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 工作流定义：驾驭工程中「固定 DAG」的声明式载体，由 classpath {@code workflows/*.json} 加载并注册到
 * {@link cn.lysoy.jingu3.engine.workflow.WorkflowDefinitionRegistry}。
 * <p>与 {@link cn.lysoy.jingu3.engine.ActionMode#WORKFLOW} 配合：客户端传 {@code workflowId} 命中本 {@code id} 后，按 {@code nodes}
 * 顺序执行各节点（含工具、子 LLM 等），属于可审计、可重复的编排，而非单轮 ReAct 的自由工具链。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkflowDefinition {

    /** 工作流唯一标识，与对话请求中的 {@code workflowId} 对应。 */
    private String id;
    /** 有序节点列表，定义本工作流的执行拓扑与每步行为。 */
    private List<WorkflowNode> nodes;
}
