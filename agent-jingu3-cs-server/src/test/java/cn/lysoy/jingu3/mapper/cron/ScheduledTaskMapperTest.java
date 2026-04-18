package cn.lysoy.jingu3.mapper.cron;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.util.UtcTime;
import cn.lysoy.jingu3.job.ScheduledTaskScope;
import cn.lysoy.jingu3.job.entity.ScheduledTaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class ScheduledTaskMapperTest {

    @Autowired
    private ScheduledTaskMapper scheduledTaskMapper;

    @Test
    void findDueIds_returnsEnabledPastNextRun() {
        Instant nowInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ScheduledTaskEntity due = newEntity(nowInstant.minusSeconds(60), true);
        ScheduledTaskEntity future = newEntity(nowInstant.plus(1, ChronoUnit.HOURS), true);
        ScheduledTaskEntity disabled = newEntity(nowInstant.minusSeconds(30), false);
        scheduledTaskMapper.insert(due);
        scheduledTaskMapper.insert(future);
        scheduledTaskMapper.insert(disabled);

        List<Long> ids = scheduledTaskMapper.selectDueIds(UtcTime.fromInstant(nowInstant), 10);
        assertThat(ids).contains(due.getId()).doesNotContain(future.getId(), disabled.getId());
    }

    private static ScheduledTaskEntity newEntity(Instant nextRun, boolean enabled) {
        ScheduledTaskEntity e = new ScheduledTaskEntity();
        e.setOwnerUserId("001");
        e.setScope(ScheduledTaskScope.GLOBAL);
        e.setNextRunAt(UtcTime.fromInstant(nextRun));
        e.setPayloadJson("{\"message\":\"hi\"}");
        e.setEnabled(enabled);
        var n = UtcTime.nowLocalDateTime();
        e.setCreatedAt(n);
        e.setUpdatedAt(n);
        return e;
    }
}
