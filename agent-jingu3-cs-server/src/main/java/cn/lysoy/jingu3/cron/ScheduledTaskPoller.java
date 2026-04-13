package cn.lysoy.jingu3.cron;

import cn.lysoy.jingu3.cron.repo.ScheduledTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
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

    private final ScheduledTaskRepository repository;
    private final ScheduledTaskDueRunner dueRunner;

    public ScheduledTaskPoller(ScheduledTaskRepository repository, ScheduledTaskDueRunner dueRunner) {
        this.repository = repository;
        this.dueRunner = dueRunner;
    }

    @Scheduled(fixedDelayString = "${jingu3.cron.poll-interval-ms:30000}")
    public void poll() {
        List<Long> ids = repository.findDueIds(Instant.now(), PageRequest.of(0, BATCH));
        for (Long id : ids) {
            try {
                dueRunner.runDue(id);
            } catch (Exception ex) {
                log.warn("轮询执行定时任务异常 taskId={}", id, ex);
            }
        }
    }
}
