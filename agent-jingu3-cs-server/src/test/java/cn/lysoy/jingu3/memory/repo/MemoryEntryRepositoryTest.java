package cn.lysoy.jingu3.memory.repo;

import cn.lysoy.jingu3.memory.MemoryEntryKind;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true"
        })
class MemoryEntryRepositoryTest {

    @Autowired
    private MemoryEntryRepository repository;

    @Test
    void findByUserIdOrder() {
        MemoryEntryEntity a = entry("001", MemoryEntryKind.EVENT, "a");
        MemoryEntryEntity b = entry("001", MemoryEntryKind.FACT, "b");
        MemoryEntryEntity c = entry("002", MemoryEntryKind.FACT, "c");
        repository.save(a);
        repository.save(b);
        repository.save(c);

        List<MemoryEntryEntity> list = repository.findByUserIdOrderByCreatedAtDesc("001", PageRequest.of(0, 10));
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
        Instant n = Instant.now();
        e.setCreatedAt(n);
        e.setUpdatedAt(n);
        return e;
    }
}
