package cn.lysoy.jingu3.rag.cache;

import cn.lysoy.jingu3.common.vo.MemoryEntryVo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "jingu3.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMemoryEntryListCache implements MemoryEntryListCache {

    @Override
    public Optional<List<MemoryEntryVo>> get(String userId, int maxListSize) {
        return Optional.empty();
    }

    @Override
    public void put(String userId, int maxListSize, List<MemoryEntryVo> list) {
        // no-op
    }

    @Override
    public void evictListForUser(String userId) {
        // no-op
    }
}
