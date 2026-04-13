package cn.lysoy.jingu3.memory.injection;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 对话前记忆片段注入（受 {@code jingu3.memory.injection-enabled} 与 Milvus 开关约束）。
 */
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
        return svc.augmentUserMessage(rawMessage, userId);
    }
}
