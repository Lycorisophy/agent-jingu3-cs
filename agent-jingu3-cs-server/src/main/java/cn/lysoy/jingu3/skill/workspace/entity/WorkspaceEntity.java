package cn.lysoy.jingu3.skill.workspace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("workspace")
@Getter
@Setter
public class WorkspaceEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("root_path")
    private String rootPath;

    private String name;

    @TableField("quota_mb")
    private Long quotaMb;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
