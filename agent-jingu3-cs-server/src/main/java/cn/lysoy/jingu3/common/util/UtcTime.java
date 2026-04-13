package cn.lysoy.jingu3.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 持久化时间统一按 UTC 的 {@link LocalDateTime} 与 API 层 {@link Instant} 互转。
 */
public final class UtcTime {

    private UtcTime() {}

    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    }

    public static LocalDateTime fromInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Instant toInstant(LocalDateTime ldt) {
        if (ldt == null) {
            return null;
        }
        return ldt.toInstant(ZoneOffset.UTC);
    }
}
