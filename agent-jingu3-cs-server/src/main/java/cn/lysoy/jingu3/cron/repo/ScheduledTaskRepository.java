package cn.lysoy.jingu3.cron.repo;

import cn.lysoy.jingu3.cron.entity.ScheduledTaskEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScheduledTaskRepository extends JpaRepository<ScheduledTaskEntity, Long> {

    List<ScheduledTaskEntity> findByOwnerUserIdOrderByNextRunAtAsc(String ownerUserId);

    @Query(
            "SELECT t.id FROM ScheduledTaskEntity t WHERE t.enabled = true AND t.nextRunAt <= :now ORDER BY t.nextRunAt ASC")
    List<Long> findDueIds(@Param("now") Instant now, Pageable pageable);
}
