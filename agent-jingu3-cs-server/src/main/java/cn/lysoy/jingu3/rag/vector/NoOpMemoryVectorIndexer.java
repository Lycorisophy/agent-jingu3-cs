package cn.lysoy.jingu3.rag.vector;

import cn.lysoy.jingu3.rag.entity.MemoryEntryEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "jingu3.milvus", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMemoryVectorIndexer implements MemoryVectorIndexer {

    @Override
    public void afterMemoryCreated(MemoryEntryEntity entity) {
        // no-op
    }

    @Override
    public void afterMemoryUpdated(MemoryEntryEntity entity) {
        // no-op
    }

    @Override
    public void onMemoryDeleted(long memoryEntryId) {
        // no-op
    }
}
