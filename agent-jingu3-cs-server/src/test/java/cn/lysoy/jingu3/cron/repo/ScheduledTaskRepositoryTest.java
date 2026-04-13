package cn.lysoy.jingu3.cron.repo;

import cn.lysoy.jingu3.cron.ScheduledTaskScope;
import cn.lysoy.jingu3.cron.entity.ScheduledTaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true"
        })
class ScheduledTaskRepositoryTest {

    @Autowired
    private ScheduledTaskRepository repository;

    @Test
    void findDueIds_returnsEnabledPastNextRun() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ScheduledTaskEntity due = newEntity(now.minusSeconds(60), true);
        ScheduledTaskEntity future = newEntity(now.plusHours(1), true);
        ScheduledTaskEntity disabled = newEntity(now.minusSeconds(30), false);
        repository.save(due);
        repository.save(future);
        repository.save(disabled);

        List<Long> ids = repository.findDueIds(now, PageRequest.of(0, 10));
        assertThat(ids).contains(due.getId()).doesNotContain(future.getId(), disabled.getId());
    }

    private static ScheduledTaskEntity newEntity(Instant nextRun, boolean enabled) {
        ScheduledTaskEntity e = new ScheduledTaskEntity();
        e.setOwnerUserId("001");
        e.setScope(ScheduledTaskScope.GLOBAL);
        e.setNextRunAt(nextRun);
        e.setPayloadJson("{\"message\":\"hi\"}");
        e.setEnabled(enabled);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return e;
    }
}
