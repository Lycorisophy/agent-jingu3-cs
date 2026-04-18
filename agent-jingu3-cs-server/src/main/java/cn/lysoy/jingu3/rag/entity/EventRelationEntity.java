package cn.lysoy.jingu3.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("event_relation")
@Getter
@Setter
public class EventRelationEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("event_a_id")
    private Long eventAId;

    @TableField("event_b_id")
    private Long eventBId;

    @TableField("rel_kind")
    private String relKind;

    private String explanation;

    private Double confidence;

    private String source;

    @TableField("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
