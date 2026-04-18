package cn.lysoy.jingu3.service.prompt;

import cn.lysoy.jingu3.common.constant.PromptFragments;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * 将服务端时间与客户端平台标识拼入送入大模型的用户正文前（在记忆增强之后）。
 */
public final class UserPromptFormatter {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private UserPromptFormatter() {
    }

    /**
     * @param memoryAugmentedBody 已做记忆注入后的用户侧文本（或原句）
     * @param serverTimeUtc       服务端采集的 UTC 时间
     * @param clientPlatform      客户端平台，空则记为 unknown
     */
    public static String buildMessageForLlm(String memoryAugmentedBody, Instant serverTimeUtc, String clientPlatform) {
        String body = memoryAugmentedBody == null ? "" : memoryAugmentedBody;
        String p =
                clientPlatform == null || clientPlatform.isBlank() ? "unknown" : clientPlatform.trim();
        return PromptFragments.USER_STANDARD_TIME_LABEL
                + ISO.format(serverTimeUtc)
                + "\n"
                + PromptFragments.USER_CLIENT_PLATFORM_LABEL
                + p
                + PromptFragments.PARAGRAPH_BREAK
                + body;
    }
}
