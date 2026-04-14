package cn.lysoy.jingu3.memory.injection;

import cn.lysoy.jingu3.config.Jingu3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 对话前记忆片段注入（受 {@code jingu3.memory.injection-enabled} 与 Milvus 开关约束）。
 */
@Slf4j
@Service
public class MemoryAugmentationService {

    private final Jingu3Properties properties;

    private final ObjectProvider<MilvusMemoryRetrievalService> milvusRetrieval;

    public MemoryAugmentationService(
            Jingu3Properties properties, ObjectProvider<MilvusMemoryRetrievalService> milvusRetrieval) {
        this.properties = properties;
        this.milvusRetrieval = milvusRetrieval;
    }

    public String augmentUserMessageIfEnabled(String rawMessage, String userId) {
        if (!properties.getMemory().isInjectionEnabled()) {
            return rawMessage;
        }
        MilvusMemoryRetrievalService svc = milvusRetrieval.getIfAvailable();
        if (svc == null) {
            return rawMessage;
        }
        try {
            return svc.augmentUserMessage(rawMessage, userId);
        } catch (Exception ex) {
            log.warn("记忆检索注入外层降级为原文 userId={}", userId, ex);
            return rawMessage;
        }
    }
}
