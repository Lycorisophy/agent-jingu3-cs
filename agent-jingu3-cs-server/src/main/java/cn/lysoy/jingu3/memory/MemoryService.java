package cn.lysoy.jingu3.memory;

import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.dto.UpdateMemoryEntryRequest;
import cn.lysoy.jingu3.common.vo.MemoryEntryVo;

import java.util.List;

/**
 * 记忆域门面（v0.6 起）；M1 提供写入与按用户列表，未接入 ChatService。
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
