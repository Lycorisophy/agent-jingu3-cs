package cn.lysoy.jingu3.dst.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("dialogue_state")
@Getter
@Setter
public class DialogueStateEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("schema_version")
    private String schemaVersion = "1";

    @TableField("state_json")
    private String stateJson;

    private Long revision = 0L;

    @TableField("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
