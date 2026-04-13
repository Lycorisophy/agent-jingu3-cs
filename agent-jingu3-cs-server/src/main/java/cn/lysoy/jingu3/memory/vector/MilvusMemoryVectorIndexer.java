package cn.lysoy.jingu3.memory.vector;

import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.memory.entity.MemoryEmbeddingEntity;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.memory.mapper.MemoryEmbeddingMapper;
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

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
