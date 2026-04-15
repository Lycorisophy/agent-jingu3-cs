package cn.lysoy.jingu3.service.memory;

import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.dto.UpdateMemoryEntryRequest;
import cn.lysoy.jingu3.common.vo.MemoryEntryVo;

import java.util.List;

/**
 * <strong>记忆与知识系统</strong>（路线图 v0.6+）对 REST 暴露的<strong>应用门面</strong>：负责结构化记忆条目的增删改查、
 * FACT 确认等；<strong>与对话主链路的关系</strong>：在线「检索注入」走 {@link cn.lysoy.jingu3.service.memory.MemoryAugmentationService}
 * + Milvus，而非本接口的同步 CRUD。
 * <p>实现类 {@link cn.lysoy.jingu3.service.memory.DefaultMemoryService} 协调 MyBatis Mapper、向量索引器等；具体表结构与物化清单见
 * {@code docs/设计/} 下记忆相关文档。</p>
 */
public interface MemoryService {

    MemoryEntryVo create(CreateMemoryEntryRequest request);

    List<MemoryEntryVo> listByUserId(String userId);

    MemoryEntryVo update(long id, UpdateMemoryEntryRequest request);

    void delete(long id, String userId);

    /**
     * 将 kind=FACT 的条目标记为已确认（写入 {@code fact_metadata.confirmed_at}）；若无元数据行则补建默认短期行。
     */
    MemoryEntryVo confirmFact(long id, String userId);
}
