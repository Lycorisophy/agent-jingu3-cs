package cn.lysoy.jingu3.events;

import cn.lysoy.jingu3.events.model.EventExtractLlmResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步落库（仅打日志失败，不抛到调用方）。
 */
@Slf4j
@Service
@ConditionalOnBean(EventPersistenceService.class)
public class EventAsyncPersistService {

    private final EventPersistenceService eventPersistenceService;

    public EventAsyncPersistService(EventPersistenceService eventPersistenceService) {
        this.eventPersistenceService = eventPersistenceService;
    }

    @Async
    public void persistExtractAsync(EventExtractLlmResult result, String userId) {
        try {
            eventPersistenceService.persistFromExtract(result, userId);
        } catch (Exception ex) {
            log.error("异步事件落库失败 userId={}", userId, ex);
        }
    }
}
