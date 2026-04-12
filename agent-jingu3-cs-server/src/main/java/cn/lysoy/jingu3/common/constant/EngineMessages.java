package cn.lysoy.jingu3.common.constant;

/**
 * 引擎层对外/对内文案与校验提示（非 LLM 提示词）。
 */
public final class EngineMessages {

    public static final String MODE_CANNOT_BE_BLANK = "mode 不能为空";

    /**
     * Stub 占位回复模板：参数依次为 mode、username、userId。
     */
    public static final String STUB_REPLY_TEMPLATE =
            "[v0.1 Stub] 模式 %s 尚未实现业务闭环，请等待后续版本。用户=%s(%s)";

    private EngineMessages() {
    }
}
