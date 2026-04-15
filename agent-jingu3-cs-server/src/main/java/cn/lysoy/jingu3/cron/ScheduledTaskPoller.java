package cn.lysoy.jingu3.cron;

import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.persistence.mapper.cron.ScheduledTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 轮询到期任务并交由 {@link ScheduledTaskDueRunner} 执行（MVP：单机、无分布式锁）。
 */
@Component
@ConditionalOnProperty(
        prefix = "jingu3.cron",
        name = "scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true)
@Slf4j
public class ScheduledTaskPoller {

    private static final int BATCH = 32;

    private final ScheduledTaskMapper scheduledTaskMapper;
    private final ScheduledTaskDueRunner dueRunner;

    public ScheduledTaskPoller(ScheduledTaskMapper scheduledTaskMapper, ScheduledTaskDueRunner dueRunner) {
        this.scheduledTaskMapper = scheduledTaskMapper;
        this.dueRunner = dueRunner;
    }

    @Scheduled(fixedDelayString = "${jingu3.cron.poll-interval-ms:30000}")
    public void poll() {
        List<Long> ids = scheduledTaskMapper.selectDueIds(UtcTime.nowLocalDateTime(), BATCH);
        for (Long id : ids) {
            try {
                dueRunner.runDue(id);
            } catch (Exception ex) {
                log.warn("轮询执行定时任务异常 taskId={}", id, ex);
            }
        }
    }
}
