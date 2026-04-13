package cn.lysoy.jingu3.chat;

import cn.lysoy.jingu3.chat.entity.UserPromptCipherEntity;
import cn.lysoy.jingu3.chat.mapper.UserPromptCipherMapper;
import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.crypto.UserPromptAesCipher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 将<strong>原始</strong>用户输入（非送模格式化后文本）以对称密钥加密后落库；未配置密钥或关闭开关时跳过。
 */
@Slf4j
@Service
public class UserPromptCipherPersistenceService {

    private static final String LOG_SKIP_NO_KEY =
            "jingu3.chat.persist-user-prompt 已开启但 jingu3.crypto.user-prompt-aes-key-base64 未配置，跳过落库";

    private final Jingu3Properties jingu3Properties;

    private final UserPromptCipherMapper userPromptCipherMapper;

    public UserPromptCipherPersistenceService(
            Jingu3Properties jingu3Properties, UserPromptCipherMapper userPromptCipherMapper) {
        this.jingu3Properties = jingu3Properties;
        this.userPromptCipherMapper = userPromptCipherMapper;
    }

    /**
     * @param rawUserMessage 路由/记忆语义使用的原始 {@link cn.lysoy.jingu3.common.dto.ChatRequest#getMessage()}
     */
    public void tryPersistRawUserMessage(String userId, String conversationIdOrNull, String rawUserMessage) {
        if (!jingu3Properties.getChat().isPersistUserPrompt()) {
            return;
        }
        if (rawUserMessage == null || rawUserMessage.isEmpty()) {
            return;
        }
        String keyB64 = jingu3Properties.getCrypto().getUserPromptAesKeyBase64();
        if (keyB64 == null || keyB64.isBlank()) {
            log.warn(LOG_SKIP_NO_KEY);
            return;
        }
        try {
            byte[] key = UserPromptAesCipher.decodeKey256FromBase64(keyB64);
            String cipherB64 = UserPromptAesCipher.encryptUtf8ToBase64(rawUserMessage, key);
            UserPromptCipherEntity row = new UserPromptCipherEntity();
            row.setUserId(userId);
            row.setConversationId(
                    conversationIdOrNull == null || conversationIdOrNull.isBlank()
                            ? ConversationConstants.DEFAULT_CONVERSATION_ID
                            : conversationIdOrNull.trim());
            row.setCiphertextB64(cipherB64);
            userPromptCipherMapper.insert(row);
        } catch (Exception ex) {
            log.warn("user prompt cipher persist failed userId={}", userId, ex);
        }
    }
}
