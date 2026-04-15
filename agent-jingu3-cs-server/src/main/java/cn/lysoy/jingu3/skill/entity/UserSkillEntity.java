package cn.lysoy.jingu3.skill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
