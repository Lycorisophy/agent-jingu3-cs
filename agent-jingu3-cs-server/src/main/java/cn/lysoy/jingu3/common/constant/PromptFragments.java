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

    private PromptFragments() {
    }
}
