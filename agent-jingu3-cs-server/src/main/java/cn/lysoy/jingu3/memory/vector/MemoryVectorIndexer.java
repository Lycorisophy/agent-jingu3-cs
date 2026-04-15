package cn.lysoy.jingu3.memory.vector;

import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;

/**
 * <strong>记忆条目与向量索引一致性</strong>（记忆与知识系统）：由 {@link cn.lysoy.jingu3.service.memory.DefaultMemoryService}
 * 在事务边界外或成功后调用，将关系库中的 {@link MemoryEntryEntity} 同步到 Milvus（或关闭向量时的空实现）。
 * <p>与 {@link MilvusMemoryVectorService} 的关系：Indexer 侧重「业务事件 → 向量行」编排，底层 DML 可由 Milvus SDK 封装。</p>
 */
public interface MemoryVectorIndexer {

    /** 新建记忆且需检索时：插入向量一行（含嵌入） */
    void afterMemoryCreated(MemoryEntryEntity entity);

    /** 记忆正文变更后重建向量索引（Milvus 关闭时为 no-op）。 */
    void afterMemoryUpdated(MemoryEntryEntity entity);

    /** 删除关系库行之前移除向量侧数据（Milvus 关闭时为 no-op）。 */
    void onMemoryDeleted(long memoryEntryId);
}
