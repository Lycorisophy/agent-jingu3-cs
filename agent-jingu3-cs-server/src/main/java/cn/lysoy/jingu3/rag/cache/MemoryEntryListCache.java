package cn.lysoy.jingu3.rag.cache;

import cn.lysoy.jingu3.common.vo.MemoryEntryVo;

import java.util.List;
import java.util.Optional;

/**
 * 按用户记忆列表的进程外缓存（Redis）；关闭 Redis 时为 no-op。
 */
public interface MemoryEntryListCache {

    Optional<List<MemoryEntryVo>> get(String userId, int maxListSize);

    void put(String userId, int maxListSize, List<MemoryEntryVo> list);

    void evictListForUser(String userId);
}
