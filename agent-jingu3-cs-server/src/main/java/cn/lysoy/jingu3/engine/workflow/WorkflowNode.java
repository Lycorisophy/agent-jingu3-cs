package cn.lysoy.jingu3.engine.workflow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 工作流图中的一个节点（指南 §6：预设流程节点）。
 * <p>v0.4：{@code type} 为 {@code TOOL} 时 {@link #toolId} 必填，{@link #instruction} 作为工具单字符串参数；缺省 {@code type} 视为
 * LLM 节点。</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class WorkflowNode {

    private String id;
    /**
     * 节点类型：{@code LLM}（默认）或 {@code TOOL}；JSON 缺省或空白时按 LLM 处理。
     */
    private String type;
    /**
     * {@code TOOL} 节点必填，对应 {@link cn.lysoy.jingu3.tool.ToolRegistry#execute(String, String)} 的 id。
     */
    private String toolId;
    /** LLM：拼入提示的节点指令；TOOL：传给工具的 input 字符串 */
    private String instruction;

    /** 是否按工具节点执行（不区分大小写） */
    public boolean isToolNode() {
        return type != null && "TOOL".equalsIgnoreCase(type.trim());
    }
}
