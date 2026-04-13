package cn.lysoy.jingu3.cron;

import cn.lysoy.jingu3.common.dto.ChatRequest;
import cn.lysoy.jingu3.common.trace.SnowflakeIdGenerator;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.cron.dto.CronTaskPayloadDto;
import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.cron.entity.ScheduledTaskEntity;
import cn.lysoy.jingu3.cron.mapper.ScheduledTaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 单条到期任务：独立事务内更新状态；对话通过 {@link CronChatBridge} 与本事务隔离。
 */
@Slf4j
@Service
public class ScheduledTaskDueRunner {

    private final ScheduledTaskMapper scheduledTaskMapper;
    private final UserConstants userConstants;
    private final ObjectMapper objectMapper;
    private final CronChatBridge cronChatBridge;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public ScheduledTaskDueRunner(
            ScheduledTaskMapper scheduledTaskMapper,
            UserConstants userConstants,
            ObjectMapper objectMapper,
            CronChatBridge cronChatBridge,
            SnowflakeIdGenerator snowflakeIdGenerator) {
        this.scheduledTaskMapper = scheduledTaskMapper;
        this.userConstants = userConstants;
        this.objectMapper = objectMapper;
        this.cronChatBridge = cronChatBridge;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @Transactional(rollbackFor = Exception.class)
    public void runDue(long taskId) {
        ScheduledTaskEntity task = scheduledTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        LocalDateTime nowLdt = UtcTime.nowLocalDateTime();
        if (!task.isEnabled() || task.getNextRunAt() == null || task.getNextRunAt().isAfter(nowLdt)) {
            return;
        }
        if (!userConstants.getId().equals(task.getOwnerUserId())) {
            log.warn("跳过非当前用户的到期任务 taskId={} owner={}", taskId, task.getOwnerUserId());
            return;
        }

        CronTaskPayloadDto payload;
        try {
            payload = objectMapper.readValue(task.getPayloadJson(), CronTaskPayloadDto.class);
        } catch (Exception ex) {
            log.error("定时任务 payload 解析失败 taskId={}", taskId, ex);
            finish(task, nowLdt, "FAILED", ScheduledTaskService.truncateError("payload 解析失败: " + ex.getMessage()));
            return;
        }

        ChatRequest req = new ChatRequest();
        req.setMessage(payload.getMessage());
        req.setMode(payload.getMode());
        req.setModePlan(payload.getModePlan());
        req.setWorkflowId(payload.getWorkflowId());
        req.setConversationId(
                ScheduledTaskService.resolveConversationId(task, payload.getConversationId()));
        req.setRequestId(String.valueOf(snowflakeIdGenerator.nextId()));
        req.setClientPlatform("cron");

        try {
            cronChatBridge.invoke(req);
            finish(task, nowLdt, "SUCCESS", null);
            log.info("定时任务执行成功 taskId={} conv={}", taskId, req.getConversationId());
        } catch (Exception ex) {
            log.warn("定时任务执行失败 taskId={}", taskId, ex);
            finish(task, nowLdt, "FAILED", ScheduledTaskService.truncateError(ex.getMessage()));
        }
    }

    private void finish(ScheduledTaskEntity task, LocalDateTime now, String status, String lastError) {
        task.setLastRunAt(now);
        task.setLastStatus(status);
        task.setLastError(lastError);
        task.setEnabled(false);
        task.setUpdatedAt(now);
        scheduledTaskMapper.updateById(task);
    }
}
