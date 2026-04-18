package cn.lysoy.jingu3.skill.tool;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 返回当前 UTC 时间的 ISO-8601 字符串，用于验证「非臆造」观察与日志对齐。
 */
@Component
public class UtcTimeTool implements Jingu3Tool {


    @Override
    public String id() {
        return "utc_time";
    }

    @Override
    public String description() {
        return "返回当前 UTC 时间的 ISO-8601 文本；input 可忽略。";
    }

    @Override
    public String execute(String input) {
        // 无参工具，忽略 input
        Instant now = Instant.now();
        return DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(now);
    }
}
