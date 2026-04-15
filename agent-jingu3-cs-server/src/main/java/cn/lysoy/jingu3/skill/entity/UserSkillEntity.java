package cn.lysoy.jingu3.skill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户与技能的订阅关系：谁在何时以何种版本持有某技能；支持后续「外部技能路径」扩展字段。
 */
@TableName("user_skill")
@Getter
@Setter
public class UserSkillEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("skill_id")
    private String skillId;

    /** 订阅是否有效等，与 {@link cn.lysoy.jingu3.skill.constant.SkillStatuses} 对齐。 */
    private String status;

    @TableField("local_version")
    private String localVersion;

    @TableField("server_version")
    private String serverVersion;

    @TableField("is_external")
    private Boolean isExternal;

    @TableField("external_path")
    private String externalPath;

    @TableField("last_sync_at")
    private LocalDateTime lastSyncAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
