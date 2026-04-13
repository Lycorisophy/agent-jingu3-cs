package cn.lysoy.jingu3.memory.mapper;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.memory.MemoryEntryKind;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class MemoryEntryMapperTest {

    @Autowired
    private MemoryEntryMapper memoryEntryMapper;

    @Test
    void findByUserIdOrder() {
        MemoryEntryEntity a = entry("001", MemoryEntryKind.EVENT, "a");
        MemoryEntryEntity b = entry("001", MemoryEntryKind.FACT, "b");
        MemoryEntryEntity c = entry("002", MemoryEntryKind.FACT, "c");
        memoryEntryMapper.insert(a);
        memoryEntryMapper.insert(b);
        memoryEntryMapper.insert(c);

        List<MemoryEntryEntity> list = memoryEntryMapper.selectByUserIdOrderByCreatedAtDesc("001", 10);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getSummary()).isEqualTo("b");
        assertThat(list.get(1).getSummary()).isEqualTo("a");
    }

    private static MemoryEntryEntity entry(String userId, MemoryEntryKind kind, String summary) {
        MemoryEntryEntity e = new MemoryEntryEntity();
        e.setUserId(userId);
        e.setKind(kind);
        e.setSummary(summary);
        e.setBody("x");
        var n = UtcTime.nowLocalDateTime();
        e.setCreatedAt(n);
        e.setUpdatedAt(n);
        return e;
    }
}
