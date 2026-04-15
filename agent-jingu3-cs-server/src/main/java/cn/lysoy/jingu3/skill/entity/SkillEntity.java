package cn.lysoy.jingu3.skill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 技能包主表实体：一条记录对应一个可订阅的技能（元数据 + 存储路径），与 {@code user_skill} 多对多用户关系。
 * 状态字符串见 {@link cn.lysoy.jingu3.skill.constant.SkillStatuses}。
 */
@TableName("skill")
@Getter
@Setter
public class SkillEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    /** URL 友好标识，对外查询与路由常用。 */
    private String slug;

    private String description;
    /** 技能包语义版本，订阅时可与 local/server 版本对齐。 */
    private String version;

    private String category;
    /** 逗号分隔等自由格式标签，供展示与检索。 */
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

    /** 与 Flyway 种子一致的业务状态，如 {@link cn.lysoy.jingu3.skill.constant.SkillStatuses#ACTIVE}。 */
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
