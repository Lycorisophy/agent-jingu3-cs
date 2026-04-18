package cn.lysoy.jingu3.rag;

import java.util.Locale;

/**
 * 事实记忆时间维度（v0.6；与 PRD 永驻/长期/短期对齐的枚举占位）。
 */
public enum FactTemporalTier {

    /** 不参与短期衰减策略（产品后续可定义具体行为） */
    PERMANENT,

    /** 中期保留 */
    LONG_TERM,

    /** 默认：短期 */
    SHORT_TERM;

    /**
     * @param raw API 大写枚举名；空则 {@link #SHORT_TERM}
     * @throws IllegalArgumentException 非法取值
     */
    public static FactTemporalTier parseRequired(String raw) {
        if (raw == null || raw.isBlank()) {
            return SHORT_TERM;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("temporalTier 须为 PERMANENT、LONG_TERM 或 SHORT_TERM");
        }
    }
}
