package cn.lysoy.jingu3.cron.entity;

import cn.lysoy.jingu3.cron.ScheduledTaskScope;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 持久化定时任务（MVP：单次执行，到点后调用 {@link cn.lysoy.jingu3.service.chat.ChatService}）。
 */
@TableName("scheduled_task")
@Getter
@Setter
public class ScheduledTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("owner_user_id")
    private String ownerUserId;

    private ScheduledTaskScope scope;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("cron_expression")
    private String cronExpression;

    @TableField("next_run_at")
    private LocalDateTime nextRunAt;

    @TableField("payload_json")
    private String payloadJson;

    private boolean enabled = true;

    @TableField("last_run_at")
    private LocalDateTime lastRunAt;

    @TableField("last_status")
    private String lastStatus;

    @TableField("last_error")
    private String lastError;

    @TableField("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @TableField("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

}
