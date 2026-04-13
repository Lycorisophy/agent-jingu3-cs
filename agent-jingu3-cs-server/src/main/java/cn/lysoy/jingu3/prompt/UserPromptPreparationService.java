package cn.lysoy.jingu3.prompt;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.memory.injection.MemoryAugmentationService;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 记忆增强后，为送入 LLM 的用户侧文本附加标准时间与平台标识。
 */
@Service
public class UserPromptPreparationService {

    private final MemoryAugmentationService memoryAugmentationService;

    public UserPromptPreparationService(MemoryAugmentationService memoryAugmentationService) {
        this.memoryAugmentationService = memoryAugmentationService;
    }

    public String prepare(ChatRequest request, String userId, Instant serverTimeUtc) {
        String augmented = memoryAugmentationService.augmentUserMessageIfEnabled(request.getMessage(), userId);
        return UserPromptFormatter.buildMessageForLlm(augmented, serverTimeUtc, request.getClientPlatform());
    }
}
