package cn.lysoy.jingu3.rag.vector;

import cn.lysoy.jingu3.util.UtcTime;
import cn.lysoy.jingu3.rag.service.MilvusMemoryVectorService;
import cn.lysoy.jingu3.rag.entity.MemoryEmbeddingEntity;
import cn.lysoy.jingu3.rag.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.rag.integration.embedding.OllamaEmbeddingClient;
import cn.lysoy.jingu3.mapper.memory.MemoryEmbeddingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 创建记忆后写入 Milvus，并标记 {@code memory_embedding}。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "true")
public class MilvusMemoryVectorIndexer implements MemoryVectorIndexer {

    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    private final MilvusMemoryVectorService milvusMemoryVectorService;

    private final MemoryEmbeddingMapper memoryEmbeddingMapper;

    public MilvusMemoryVectorIndexer(
            OllamaEmbeddingClient ollamaEmbeddingClient,
            MilvusMemoryVectorService milvusMemoryVectorService,
            MemoryEmbeddingMapper memoryEmbeddingMapper) {
        this.ollamaEmbeddingClient = ollamaEmbeddingClient;
        this.milvusMemoryVectorService = milvusMemoryVectorService;
        this.memoryEmbeddingMapper = memoryEmbeddingMapper;
    }

    @Override
    public void afterMemoryCreated(MemoryEntryEntity entity) {
        String text =
                nullToEmpty(entity.getSummary()) + "\n" + nullToEmpty(entity.getBody());
        if (text.isBlank()) {
            return;
        }
        try {
            float[] vec = ollamaEmbeddingClient.embed(text);
            milvusMemoryVectorService.insertVector(entity.getId(), entity.getUserId(), vec);
            MemoryEmbeddingEntity row = new MemoryEmbeddingEntity();
            row.setMemoryEntryId(entity.getId());
            row.setUpdatedAt(UtcTime.nowLocalDateTime());
            memoryEmbeddingMapper.insert(row);
        } catch (Exception ex) {
            log.warn("记忆向量索引失败 entryId={}", entity.getId(), ex);
        }
    }

    @Override
    public void afterMemoryUpdated(MemoryEntryEntity entity) {
        removeIndexedVectors(entity.getId());
        afterMemoryCreated(entity);
    }

    @Override
    public void onMemoryDeleted(long memoryEntryId) {
        try {
            milvusMemoryVectorService.deleteByMemoryEntryId(memoryEntryId);
        } catch (Exception ex) {
            log.warn("Milvus 删除向量失败 entryId={}", memoryEntryId, ex);
        }
    }

    private void removeIndexedVectors(long memoryEntryId) {
        try {
            milvusMemoryVectorService.deleteByMemoryEntryId(memoryEntryId);
        } catch (Exception ex) {
            log.warn("Milvus 删除旧向量失败 entryId={}", memoryEntryId, ex);
        }
        memoryEmbeddingMapper.deleteById(memoryEntryId);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
