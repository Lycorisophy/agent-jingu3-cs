package cn.lysoy.jingu3.service.context.chat;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.vo.dst.DialogueStateVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.context.dst.DialogueStateService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 组装 {@link cn.lysoy.jingu3.service.guard.ExecutionContext#getHistory()}：STM 近期轮次 + 可选 DST 摘要行。
 */
@Component
public class ChatStmHistoryAssembler {

    /** 与 {@link cn.lysoy.jingu3.service.context.chat.ChatService} 会话占位规则一致。 */
    public static String effectiveConversationId(ChatRequest request) {
        if (request.getConversationId() == null || request.getConversationId().isBlank()) {
            return ConversationConstants.DEFAULT_CONVERSATION_ID;
        }
        return request.getConversationId().trim();
    }

    private final ConversationStmBuffer stmBuffer;
    private final DialogueStateService dialogueStateService;
    private final Jingu3Properties properties;

    public ChatStmHistoryAssembler(
            ConversationStmBuffer stmBuffer,
            DialogueStateService dialogueStateService,
            Jingu3Properties properties) {
        this.stmBuffer = stmBuffer;
        this.dialogueStateService = dialogueStateService;
        this.properties = properties;
    }

    public List<String> assemble(ChatRequest request) {
        String normalizedConversationId = effectiveConversationId(request);
        if (Boolean.TRUE.equals(request.getUndoLastStmTurn())) {
            stmBuffer.dropLastTurn(normalizedConversationId);
        }
        List<String> lines = new ArrayList<>();
        if (properties.getChat().isStmEnabled() && properties.getChat().isStmIncludeDstSnippet()) {
            Optional<DialogueStateVo> dst = dialogueStateService.findOptional(normalizedConversationId);
            if (dst.isPresent()) {
                String json = dst.get().getStateJson();
                if (json != null && !json.isBlank()) {
                    int max = Math.max(64, properties.getChat().getStmMaxDstChars());
                    String snippet = json.length() <= max ? json : json.substring(0, max) + "…";
                    lines.add("[DST状态] " + snippet);
                }
            }
        }
        lines.addAll(stmBuffer.snapshotLines(normalizedConversationId));
        return List.copyOf(lines);
    }
}
