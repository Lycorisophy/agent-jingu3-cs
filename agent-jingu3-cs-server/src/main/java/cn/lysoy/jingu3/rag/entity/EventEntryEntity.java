package cn.lysoy.jingu3.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("event_entry")
@Getter
@Setter
public class EventEntryEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private String userId;

    @TableField("conversation_id")
    private String conversationId;

    /** ISO-8601 或可解析时间串 */
    @TableField("event_time")
    private String eventTime;

    private String action;

    private String result;

    /** JSON 数组字符串 */
    private String actors;

    private String assertion;

    @TableField("event_subject")
    private String eventSubject;

    @TableField("event_location")
    private String eventLocation;

    @TableField("trigger_terms")
    private String triggerTerms;

    private String modality;

    @TableField("temporal_semantic")
    private String temporalSemantic;

    private String metadata;

    @TableField("message_id")
    private String messageId;

    @TableField("vector_id")
    private String vectorId;

    @TableField("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @TableField("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
