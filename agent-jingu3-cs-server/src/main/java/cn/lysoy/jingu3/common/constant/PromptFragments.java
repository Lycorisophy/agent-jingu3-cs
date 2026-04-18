package cn.lysoy.jingu3.common.constant;

/**
 * 提示词拼装用片段（换行、标签），避免魔法字符串散落。
 * <p>命名遵循《阿里巴巴 Java 开发手册》常量约定。</p>
 */
public final class PromptFragments {

    /**
     * 段间空行（等价于两个换行）。
     */
    public static final String PARAGRAPH_BREAK = "\n\n";

    /**
     * 「用户：」+ 换行，后接用户正文。
     */
    public static final String USER_LABEL_WITH_NEWLINE = "用户：\n";

    /**
     * 「用户输入：」+ 换行，后接用户正文（意图分类等场景）。
     */
    public static final String USER_INPUT_LABEL_WITH_NEWLINE = "用户输入：\n";

    /**
     * 编排多步时，将上一步模型输出拼入下一步 {@link cn.lysoy.jingu3.service.guard.ExecutionContext#getTaskPayload()}。
     */
    public static final String PLAN_CHAIN_SEPARATOR = "\n---\n上一步输出：\n";

    /** 用户主动纠正/补充时拼在原始 {@code message} 之前（认知对齐：可纠正性）。 */
    public static final String USER_CORRECTION_PREFIX = "【用户纠正或补充】\n";

    /** 对话前向量检索注入时的段落标题（与 {@code jingu3.memory.injection-enabled} 配合）。 */
    public static final String MEMORY_REFERENCE_HEADER = "【参考记忆】\n";

    /** 送入 LLM 的用户段前附加：标准时间（UTC，ISO-8601） */
    public static final String USER_STANDARD_TIME_LABEL = "[标准时间] ";

    /** 送入 LLM 的用户段前附加：客户端平台标识 */
    public static final String USER_CLIENT_PLATFORM_LABEL = "[平台标识] ";

    private PromptFragments() {
    }
}
