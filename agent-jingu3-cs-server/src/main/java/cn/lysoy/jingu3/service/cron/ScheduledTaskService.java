package cn.lysoy.jingu3.service.cron;

import cn.lysoy.jingu3.common.constant.ConversationConstants;
import cn.lysoy.jingu3.common.constant.CronApiMessages;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.cron.ScheduledTaskScope;
import cn.lysoy.jingu3.common.dto.cron.CreateScheduledTaskRequest;
import cn.lysoy.jingu3.common.vo.cron.ScheduledTaskVo;
import cn.lysoy.jingu3.cron.entity.ScheduledTaskEntity;
import cn.lysoy.jingu3.persistence.mapper.cron.ScheduledTaskMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ScheduledTaskService {

    private static final int LAST_ERROR_MAX = 4000;

    private final ScheduledTaskMapper scheduledTaskMapper;
    private final UserConstants userConstants;
    private final ObjectMapper objectMapper;

    public ScheduledTaskService(
            ScheduledTaskMapper scheduledTaskMapper, UserConstants userConstants, ObjectMapper objectMapper) {
        this.scheduledTaskMapper = scheduledTaskMapper;
        this.userConstants = userConstants;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ScheduledTaskVo create(CreateScheduledTaskRequest request) {
        if (request.getScope() == ScheduledTaskScope.CONVERSATION
                && (request.getConversationId() == null || request.getConversationId().isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, CronApiMessages.CONVERSATION_SCOPE_REQUIRES_CONVERSATION_ID);
        }
        if (request.getScope() == ScheduledTaskScope.GLOBAL && request.getConversationId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, CronApiMessages.GLOBAL_SCOPE_SHOULD_NOT_SET_CONVERSATION_ID);
        }
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(request.getPayload());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, CronApiMessages.PAYLOAD_SERIALIZE_FAILED);
        }
        var now = UtcTime.nowLocalDateTime();
        ScheduledTaskEntity e = new ScheduledTaskEntity();
        e.setOwnerUserId(userConstants.getId());
        e.setScope(request.getScope());
        e.setConversationId(
                request.getScope() == ScheduledTaskScope.CONVERSATION ? request.getConversationId().trim() : null);
        e.setCronExpression(request.getCronExpression());
        e.setNextRunAt(UtcTime.fromInstant(request.getNextRunAt()));
        e.setPayloadJson(payloadJson);
        e.setEnabled(true);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        scheduledTaskMapper.insert(e);
        return toVo(e);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTaskVo> listMine() {
        return scheduledTaskMapper.selectByOwnerUserIdOrderByNextRunAtAsc(userConstants.getId()).stream()
                .map(ScheduledTaskService::toVo)
                .toList();
    }

    public static String truncateError(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.length() <= LAST_ERROR_MAX ? raw : raw.substring(0, LAST_ERROR_MAX);
    }

    static ScheduledTaskVo toVo(ScheduledTaskEntity e) {
        return ScheduledTaskVo.builder()
                .id(e.getId())
                .ownerUserId(e.getOwnerUserId())
                .scope(e.getScope())
                .conversationId(e.getConversationId())
                .cronExpression(e.getCronExpression())
                .nextRunAt(UtcTime.toInstant(e.getNextRunAt()))
                .payloadJson(e.getPayloadJson())
                .enabled(e.isEnabled())
                .lastRunAt(UtcTime.toInstant(e.getLastRunAt()))
                .lastStatus(e.getLastStatus())
                .lastError(e.getLastError())
                .createdAt(UtcTime.toInstant(e.getCreatedAt()))
                .updatedAt(UtcTime.toInstant(e.getUpdatedAt()))
                .build();
    }

    public static String resolveConversationId(ScheduledTaskEntity task, String fromPayload) {
        if (task.getScope() == ScheduledTaskScope.CONVERSATION) {
            return task.getConversationId();
        }
        if (fromPayload != null && !fromPayload.isBlank()) {
            return fromPayload.trim();
        }
        return ConversationConstants.DEFAULT_CONVERSATION_ID;
    }
}
