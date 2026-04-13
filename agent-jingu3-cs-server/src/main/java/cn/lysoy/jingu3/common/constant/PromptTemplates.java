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
     * ReAct 多步循环：思考/行动/观察；工具开启时由 PromptAssembly 追加 JSON 页脚规则。
     */
    public static final String REACT_LOOP_INSTRUCTION =
            "你是 ReAct 智能体。每一步请用中文，尽量包含「思考」「行动」「观察」三段；"
                    + "若「已有过程」中出现「系统观察」，该段为工具真实输出，须以其为准撰写观察。"
                    + "若无外部反馈且本步未调用工具，观察可写「（无新观察）」。"
                    + "任务可结束时在正文末行输出 [TASK_COMPLETE]，或按下方 JSON 页脚规则输出 action 为 done。";

    /** Ask：工具路由专用；后接 {@link cn.lysoy.jingu3.tool.ToolRegistry#buildCatalogMarkdown()} */
    public static final String ASK_TOOL_ROUTER_INSTRUCTION =
            "你是工具路由助手。根据用户问题与下列内置工具，只输出一行合法 JSON（不要 Markdown、不要解释）：\n"
                    + "{\"route\":\"direct\"} 表示直接语言回答即可；\n"
                    + "{\"route\":\"tool\",\"toolId\":\"<id>\",\"input\":\"<单字符串参数>\"} 表示应先执行工具再汇总。\n"
                    + "工具列表：\n";

    /**
     * ReAct：工具开启时拼在循环指令后，约定 {@code <<<JINGU3_JSON>>>} 与 JSON 页脚（与 {@code ToolRoutingParser} 一致）。
     */
    public static final String REACT_JSON_FOOTER_RULE =
            "需要调用工具时，在回复最后另起两行：第一行单独为 <<<JINGU3_JSON>>> ，第二行为 JSON："
                    + "{\"action\":\"invoke\",\"toolId\":\"<id>\",\"input\":\"<参数>\"} ；"
                    + "若本步不再需要工具且任务可结束，第二行 JSON 为 {\"action\":\"done\"} 。";

    /**
     * Plan 子任务：工具路由；后接 {@link cn.lysoy.jingu3.tool.ToolRegistry#buildCatalogMarkdown()} 与子任务正文。
     */
    public static final String PLAN_SUBTASK_TOOL_ROUTER_INSTRUCTION =
            "你是工具路由助手。根据下列「计划子任务」与原始用户问题、内置工具列表，只输出一行合法 JSON（不要 Markdown、不要解释）：\n"
                    + "{\"route\":\"direct\"} 表示直接完成该子任务即可；\n"
                    + "{\"route\":\"tool\",\"toolId\":\"<id>\",\"input\":\"<单字符串参数>\"} 表示应先执行工具再基于结果完成子任务。\n"
                    + "工具列表：\n";

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

    /**
     * 供 {@link cn.lysoy.jingu3.prompt.ModeRoutingPreamble} 注入：对话层可选模式的自然语言释义，
     * 便于模型判断「是否应建议用户切换模式」。
     */
    public static final String MODE_CATALOG_FOR_LLM =
            "- ASK：单轮问答为主；服务端可按需路由内置工具（计算、时间等）再汇总回答。\n"
                    + "- REACT：多步「思考—行动—观察」，行动可对应真实工具执行与观察回流。\n"
                    + "- PLAN_AND_EXECUTE：先产出编号计划再逐步执行，适合步骤结构清晰的复杂任务。\n"
                    + "- WORKFLOW：按预置节点顺序执行，适合固定流水线或审批类流程。\n"
                    + "- AGENT_TEAM：主 Agent 拆分子任务并由子 Agent 执行，适合多角色分工。\n"
                    + "- CRON：定时/周期执行意图（对话侧常为说明占位，完整调度见路线图 Cron 模块）。\n"
                    + "- STATE_TRACKING：在多轮中维护显式状态、计数或进度。\n"
                    + "- HUMAN_IN_LOOP：需人工审批或确认后再继续的场景。\n";

    private PromptTemplates() {
    }
}
