package cn.lysoy.jingu3.job;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.vo.ChatVo;
import cn.lysoy.jingu3.service.context.chat.ChatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 将 {@link ChatService#chat} 与定时任务事务隔离：避免对话失败导致无法落库 {@code last_status}。
 */
@Service
public class CronChatBridge {

    private final ChatService chatService;

    public CronChatBridge(ChatService chatService) {
        this.chatService = chatService;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatVo invoke(ChatRequest request) {
        return chatService.chat(request);
    }
}
