package cn.lysoy.jingu3.skill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("skill")
@Getter
@Setter
public class SkillEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;

    private String slug;

    private String description;

    private String version;

    private String category;

    private String tags;

    @TableField("trigger_words")
    private String triggerWords;

    @TableField("icon_url")
    private String iconUrl;

    @TableField("storage_path")
    private String storagePath;

    @TableField("file_size")
    private Long fileSize;

    private String checksum;

    @TableField("author_id")
    private String authorId;

    @TableField("is_public")
    private Boolean isPublic;

    @TableField("is_official")
    private Boolean isOfficial;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
