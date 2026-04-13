package cn.lysoy.jingu3.memory.injection;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.memory.mapper.MemoryEntryMapper;
import cn.lysoy.jingu3.memory.vector.MilvusMemoryVectorService;
import cn.lysoy.jingu3.memory.vector.OllamaEmbeddingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于 Milvus 向量检索拼装注入文本。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
public class MilvusMemoryRetrievalService {

    private static final int BODY_PREVIEW_MAX = 500;

    private final Jingu3Properties properties;

    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    private final MilvusMemoryVectorService milvusMemoryVectorService;

    private final MemoryEntryMapper memoryEntryMapper;

    public MilvusMemoryRetrievalService(
            Jingu3Properties properties,
            OllamaEmbeddingClient ollamaEmbeddingClient,
            MilvusMemoryVectorService milvusMemoryVectorService,
            MemoryEntryMapper memoryEntryMapper) {
        this.properties = properties;
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.milvusMemoryVectorService = milvusMemoryVectorService;
        this.memoryEntryMapper = memoryEntryMapper;
    }

    /**
     * 将检索到的记忆摘要拼在用户消息前；失败或无结果时返回原消息。
     */
    public String augmentUserMessage(String userMessage, String userId) {
        if (userMessage == null || userMessage.isBlank()) {
            return userMessage;
        }
        String uid = userId == null ? "" : userId.trim();
        try {
            float[] q = ollamaEmbeddingClient.embed(userMessage);
            int topK = Math.max(1, properties.getMemory().getRetrievalTopK());
            List<Long> ids = milvusMemoryVectorService.searchSimilar(uid, q, topK);
            if (ids.isEmpty()) {
                return userMessage;
            }
            List<MemoryEntryEntity> rows = memoryEntryMapper.selectByIds(ids);
            if (rows.isEmpty()) {
                return userMessage;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(PromptFragments.MEMORY_REFERENCE_HEADER);
            for (MemoryEntryEntity e : rows) {
                sb.append("- ")
                        .append(e.getKind() != null ? e.getKind().name() : "?")
                        .append(": ")
                        .append(nullToEmpty(e.getSummary()))
                        .append(" | ")
                        .append(truncate(nullToEmpty(e.getBody()), BODY_PREVIEW_MAX))
                        .append('\n');
            }
            sb.append(PromptFragments.PARAGRAPH_BREAK).append(userMessage);
            return sb.toString();
        } catch (Exception ex) {
            log.warn("记忆检索注入跳过 userId={}", uid, ex);
            return userMessage;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}
