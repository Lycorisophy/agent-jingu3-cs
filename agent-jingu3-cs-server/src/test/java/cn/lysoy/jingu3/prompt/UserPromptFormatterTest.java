package cn.lysoy.jingu3.service.prompt;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class UserPromptFormatterTest {

    @Test
    void includesTimePlatformAndBody() {
        Instant t = Instant.parse("2026-04-13T08:00:00Z");
        String out = UserPromptFormatter.buildMessageForLlm("hi", t, "web");
        assertThat(out).contains("[标准时间] 2026-04-13T08:00:00Z");
        assertThat(out).contains("[平台标识] web");
        assertThat(out).endsWith("hi");
    }

    @Test
    void blankPlatform_becomesUnknown() {
        String out = UserPromptFormatter.buildMessageForLlm("x", Instant.EPOCH, "  ");
        assertThat(out).contains("[平台标识] unknown");
    }
}
