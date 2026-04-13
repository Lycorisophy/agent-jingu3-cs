package cn.lysoy.jingu3.memory.vector;

import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;

/**
 * 记忆写入后同步向量索引（Milvus）；关闭向量时为 no-op。
 */
public interface MemoryVectorIndexer {

    void afterMemoryCreated(MemoryEntryEntity entity);
}
