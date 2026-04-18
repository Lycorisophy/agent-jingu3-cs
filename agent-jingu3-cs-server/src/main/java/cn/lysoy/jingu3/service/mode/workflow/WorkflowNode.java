package cn.lysoy.jingu3.service.mode.workflow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <strong>工作流图节点</strong>（指南 §6，JSON 工作流 DSL 的最小单元）：由 {@link WorkflowDefinition} 持有顺序列表，
 * {@link cn.lysoy.jingu3.service.mode.handler.WorkflowModeHandler} 按序执行。{@link #id} 用于流式 step 标签与日志。
 * <p>v0.4 约定：{@code type=TOOL} 时 {@link #toolId} 必填，{@link #instruction} 作为 {@link cn.lysoy.jingu3.skill.tool.ToolRegistry}
 * 的单字符串入参；缺省或空白 {@code type} 视为 <strong>LLM</strong> 节点，{@link #instruction} 拼入
 * {@link cn.lysoy.jingu3.service.prompt.PromptAssembly#buildWorkflowNodePrompt}。</p>
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
     * {@code TOOL} 节点必填，对应 {@link cn.lysoy.jingu3.skill.tool.ToolRegistry#execute(String, String)} 的 id。
     */
    private String toolId;
    /** LLM：拼入提示的节点指令；TOOL：传给工具的 input 字符串 */
    private String instruction;

    /** 是否按工具节点执行（不区分大小写） */
    public boolean isToolNode() {
        return type != null && "TOOL".equalsIgnoreCase(type.trim());
    }
}
