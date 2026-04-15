package cn.lysoy.jingu3.persistence.mapper.cron;

import cn.lysoy.jingu3.cron.entity.ScheduledTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledTaskMapper extends BaseMapper<ScheduledTaskEntity> {

    @Select("SELECT * FROM scheduled_task WHERE owner_user_id = #{ownerUserId} ORDER BY next_run_at ASC")
    List<ScheduledTaskEntity> selectByOwnerUserIdOrderByNextRunAtAsc(@Param("ownerUserId") String ownerUserId);

    @Select(
            "SELECT id FROM scheduled_task WHERE enabled = TRUE AND next_run_at <= #{now} "
                    + "ORDER BY next_run_at ASC LIMIT #{limit}")
    List<Long> selectDueIds(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
