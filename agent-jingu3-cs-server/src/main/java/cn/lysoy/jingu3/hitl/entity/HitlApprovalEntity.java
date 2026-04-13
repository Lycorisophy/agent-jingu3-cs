package cn.lysoy.jingu3.hitl.entity;

import cn.lysoy.jingu3.hitl.HitlApprovalStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("hitl_approval")
@Getter
@Setter
public class HitlApprovalEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("run_id")
    private String runId;

    private HitlApprovalStatus status = HitlApprovalStatus.PENDING;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @TableField("resolved_at")
    private LocalDateTime resolvedAt;

    @TableField("resolver_user_id")
    private String resolverUserId;
}
