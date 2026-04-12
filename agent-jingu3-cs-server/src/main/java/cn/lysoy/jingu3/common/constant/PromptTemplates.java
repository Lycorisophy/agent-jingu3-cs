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
     * 意图分类器系统指令前缀；运行时在末尾拼接 {@link cn.lysoy.jingu3.engine.ActionMode} 枚举名列表。
     */
    public static final String INTENT_CLASSIFIER_SYSTEM_PREFIX =
            "你是模式分类器。根据用户输入，只输出下列之一（大写、无空格、无解释）：";

    private PromptTemplates() {
    }
}
