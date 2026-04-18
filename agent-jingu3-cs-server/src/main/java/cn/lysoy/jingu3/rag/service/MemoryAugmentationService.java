package cn.lysoy.jingu3.rag.service;

import cn.lysoy.jingu3.config.Jingu3Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * <strong>记忆检索注入</strong>（记忆与知识系统 × 上下文工程）：在 {@link cn.lysoy.jingu3.service.context.prepare.UserPromptPreparationService}
 * 调用链上，于送模前将「与用户当前句相关的记忆摘要」拼入用户消息；受 {@code jingu3.memory.injection-enabled} 与
 * Milvus 组件可用性约束，失败时<strong>降级为原文</strong>，避免阻断对话。
 */
@Slf4j
@Service
public class MemoryAugmentationService {

    /** 总开关：关闭时本服务为 no-op，便于无向量环境联调 */
    private final Jingu3Properties properties;

    /** 延迟获取：Milvus 未配置或未启用时不实例化检索实现，避免启动失败 */
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
            // 内向量检索 + 摘要拼装由 MilvusMemoryRetrievalService 实现
            return svc.augmentUserMessage(rawMessage, userId);
        } catch (Exception ex) {
            // 记忆为增强能力：任何异常不得冒泡至对话 API，保证主链路可用
            log.warn("记忆检索注入外层降级为原文 userId={}", userId, ex);
            return rawMessage;
        }
    }
}
