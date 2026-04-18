package cn.lysoy.jingu3.service.context.prepare;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.service.prompt.UserPromptFormatter;
import cn.lysoy.jingu3.rag.service.MemoryAugmentationService;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * <strong>上下文工程（用户侧送模串）</strong>：在八大行动模式与具体 Handler 执行之前，对「用户本轮输入」做统一增强。
 * <p>当前串联：<br>
 * 1）{@link MemoryAugmentationService}：在配置允许且 Milvus 检索可用时，将相关记忆片段 prepend 到用户消息；<br>
 * 2）{@link UserPromptFormatter}：附加 UTC 时间与客户端平台行，便于模型理解环境与审计对齐。</p>
 * <p>输出作为 {@link cn.lysoy.jingu3.service.guard.ExecutionContext} 中的 {@code userMessage} 传入后续提示词拼装。</p>
 */
@Service
public class UserPromptPreparationService {

    /** 记忆与知识：对话前检索注入（可降级为原文） */
    private final MemoryAugmentationService memoryAugmentationService;

    public UserPromptPreparationService(MemoryAugmentationService memoryAugmentationService) {
        this.memoryAugmentationService = memoryAugmentationService;
    }

    /**
     * @param request         原始聊天请求（取 {@link ChatRequest#getMessage()} 与客户端平台）
     * @param userId          记忆检索按用户隔离
     * @param serverTimeUtc   送模时间戳（与客户端本地时间解耦）
     * @return 进入 {@link cn.lysoy.jingu3.service.guard.ExecutionContext} 的增强后用户串
     */
    public String prepare(ChatRequest request, String userId, Instant serverTimeUtc) {
        String raw = request.getMessage();
        if (request.getCorrectionNotes() != null && !request.getCorrectionNotes().isBlank()) {
            raw = PromptFragments.USER_CORRECTION_PREFIX
                    + request.getCorrectionNotes().trim()
                    + PromptFragments.PARAGRAPH_BREAK
                    + raw;
        }
        // 记忆与知识系统：向量检索片段可选注入
        String augmented = memoryAugmentationService.augmentUserMessageIfEnabled(raw, userId);
        // 横切：时间 + 平台标识行，具体格式见 UserPromptFormatter
        return UserPromptFormatter.buildMessageForLlm(augmented, serverTimeUtc, request.getClientPlatform());
    }
}
