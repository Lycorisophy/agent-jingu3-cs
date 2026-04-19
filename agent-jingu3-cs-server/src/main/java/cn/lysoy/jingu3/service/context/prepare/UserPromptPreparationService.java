package cn.lysoy.jingu3.service.context.prepare;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.service.prompt.UserPromptFormatter;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * <strong>上下文工程（用户侧送模串）</strong>：在八大行动模式与具体 Handler 执行之前，对「用户本轮输入」做统一增强。
 * <p>当前串联：<br>
 * {@link UserPromptFormatter}：附加 UTC 时间与客户端平台行，便于模型理解环境与审计对齐。</p>
 * <p>长期记忆的向量检索不再在此注入；由内置工具 {@code memory_search} 在对话中按需调用。</p>
 * <p>输出作为 {@link cn.lysoy.jingu3.service.guard.ExecutionContext} 中的 {@code userMessage} 传入后续提示词拼装。</p>
 */
@Service
public class UserPromptPreparationService {

    /**
     * @param request         原始聊天请求（取 {@link ChatRequest#getMessage()} 与客户端平台）
     * @param userId          预留：多用户场景（当前与 {@link cn.lysoy.jingu3.component.UserConstants} 对齐）
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
        return UserPromptFormatter.buildMessageForLlm(raw, serverTimeUtc, request.getClientPlatform());
    }
}
