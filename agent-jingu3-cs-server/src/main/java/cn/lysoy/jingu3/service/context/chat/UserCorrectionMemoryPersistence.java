package cn.lysoy.jingu3.service.context.chat;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.rag.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 将用户纠正说明写入 FACT 记忆（受 {@code jingu3.chat.stm-persist-correction-memory} 与请求体开关约束）。
 */
@Slf4j
@Component
public class UserCorrectionMemoryPersistence {

    private final Jingu3Properties properties;
    private final UserConstants userConstants;
    private final ObjectProvider<MemoryService> memoryService;

    public UserCorrectionMemoryPersistence(
            Jingu3Properties properties,
            UserConstants userConstants,
            ObjectProvider<MemoryService> memoryService) {
        this.properties = properties;
        this.userConstants = userConstants;
        this.memoryService = memoryService;
    }

    public void persistIfRequested(ChatRequest request) {
        if (!properties.getChat().isStmPersistCorrectionMemory()) {
            return;
        }
        if (!Boolean.TRUE.equals(request.getPersistUserCorrectionAsMemory())) {
            return;
        }
        if (request.getCorrectionNotes() == null || request.getCorrectionNotes().isBlank()) {
            return;
        }
        MemoryService svc = memoryService.getIfAvailable();
        if (svc == null) {
            log.debug("skip correction memory: MemoryService unavailable");
            return;
        }
        try {
            CreateMemoryEntryRequest r = new CreateMemoryEntryRequest();
            r.setUserId(userConstants.getId());
            r.setKind("FACT");
            r.setSummary("用户纠正或补充");
            r.setBody(request.getCorrectionNotes().trim());
            r.setFactTag("user_correction");
            svc.create(r);
        } catch (Exception ex) {
            log.warn("persist user correction memory failed userId={}", userConstants.getId(), ex);
        }
    }
}
