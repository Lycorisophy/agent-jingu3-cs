package cn.lysoy.jingu3.memory.vector;

import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;

/**
 * 记忆写入后同步向量索引（Milvus）；关闭向量时为 no-op。
 */
public interface MemoryVectorIndexer {

    void afterMemoryCreated(MemoryEntryEntity entity);

    /** 记忆正文变更后重建向量索引（Milvus 关闭时为 no-op）。 */
    void afterMemoryUpdated(MemoryEntryEntity entity);

    /** 删除关系库行之前移除向量侧数据（Milvus 关闭时为 no-op）。 */
    void onMemoryDeleted(long memoryEntryId);
}
