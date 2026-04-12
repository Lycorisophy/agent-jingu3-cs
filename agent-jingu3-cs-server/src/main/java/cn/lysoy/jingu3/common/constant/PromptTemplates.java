package cn.lysoy.jingu3.common.constant;

/**
 * LLM 系统提示词与固定指令模板（不含运行时动态枚举列表）。
 * <p>变更提示词时仅修改本类，并同步版本文档。</p>
 */
public final class PromptTemplates {

    /**
     * Ask 模式：系统角色设定。
     */
    public static final String ASK_SYSTEM = "你是一个有帮助的中文助手，回答简洁准确。";

    /**
     * ReAct 模式：系统角色与输出约束。
     */
    public static final String REACT_SYSTEM = "你按 ReAct 风格思考：先用简短「思考」说明打算，再给出「最终答案」回复用户。"
            + "若无需工具，直接输出最终答案。使用中文。";

    /**
     * ReAct 多步循环：要求每步含思考/行动/观察；完成时单独一行输出 {@code [TASK_COMPLETE]}。
     */
    public static final String REACT_LOOP_INSTRUCTION =
            "你是 ReAct 智能体。每一步请用中文，尽量包含「思考」「行动」「观察」三段；"
                    + "若无外部环境反馈，观察可写「（无新观察）」。"
                    + "当你认为用户任务已充分完成时，在回复最后一行单独输出一行：[TASK_COMPLETE]";

    public static final String SUBTASK_EXECUTE_HEADER = "下面是计划中的一步子任务，请执行并给出该步结果（中文）：";

    public static final String REPLANNER_HEADER =
            "先前执行失败。请根据错误信息输出修订后的简短执行计划（编号列表），不要执行：";

    public static final String WORKFLOW_NODE_HEADER = "工作流节点指令：";

    /**
     * 意图分类器系统指令前缀；运行时在末尾拼接 {@link cn.lysoy.jingu3.engine.ActionMode} 枚举名列表。
     */
    public static final String INTENT_CLASSIFIER_SYSTEM_PREFIX =
            "你是模式分类器。根据用户输入，只输出下列之一（大写、无空格、无解释）：";

    /** Plan-and-Execute：仅规划阶段系统前缀。 */
    public static final String PLAN_AND_EXECUTE_PLAN_ONLY =
            "你是规划助手。根据用户问题，只输出简洁编号分步计划，不要执行。使用中文。";

    /** Plan-and-Execute：执行阶段说明（后接计划与用户问题）。 */
    public static final String PLAN_AND_EXECUTE_EXECUTE_HEADER =
            "以下是事先计划与原始用户问题。请执行计划并给出最终中文回答。";

    /** Workflow：第一步（需求摘要）。 */
    public static final String WORKFLOW_STEP1 =
            "工作流第1步（需求摘要）：请用两三句话概括用户核心诉求。";

    /** Workflow：第二步前缀（后接摘要与「用户：」段落）。 */
    public static final String WORKFLOW_STEP2_HEADER =
            "工作流第2步（方案）：基于以下摘要给出可执行方案。";

    /** Agent Team：主 Agent 只输出子任务一句。 */
    public static final String AGENT_TEAM_LEAD =
            "你是主协调 Agent。把用户问题拆成一条可交给子 Agent 执行的具体子任务（一句话）。只输出子任务描述。";

    /** Agent Team：子 Agent 前缀（后接子任务）。 */
    public static final String AGENT_TEAM_SUB_PREFIX =
            "你是子 Agent。请完成下列子任务并直接给出结果（中文简洁）。子任务：\n";

    private PromptTemplates() {
    }
}
