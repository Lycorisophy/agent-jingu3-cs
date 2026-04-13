package cn.lysoy.jingu3.cron.entity;

import cn.lysoy.jingu3.cron.ScheduledTaskScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 持久化定时任务（MVP：单次执行，到点后调用 {@link cn.lysoy.jingu3.service.ChatService}）。
 */
@Entity
@Table(name = "scheduled_task")
@Getter
@Setter
public class ScheduledTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false, length = 64)
    private String ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 32)
    private ScheduledTaskScope scope;

    @Column(name = "conversation_id", length = 128)
    private String conversationId;

    @Column(name = "cron_expression", length = 256)
    private String cronExpression;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_status", length = 64)
    private String lastStatus;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
