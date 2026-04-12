package cn.lysoy.jingu3.common.constant;

/**
 * SLF4J 占位日志文案（与 {@link org.slf4j.Logger#warn(String, Object...)} 占位符一致）。
 * <p>避免日志字符串重复，便于 Sonar 与 Qodana 维护性检查。</p>
 */
public final class LogMessagePatterns {

    public static final String INTENT_CLASSIFIER_UNPARSEABLE_OUTPUT =
            "无法解析模型输出 [{}]，降级为 REACT";

    public static final String INTENT_CLASSIFIER_FAILED = "模型意图分类失败，降级为 REACT: {}";

    private LogMessagePatterns() {
    }
}
