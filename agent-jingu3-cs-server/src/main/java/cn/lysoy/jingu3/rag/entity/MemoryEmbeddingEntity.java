package cn.lysoy.jingu3.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@TableName("memory_embedding")
@Getter
@Setter
public class MemoryEmbeddingEntity {

    @TableId(value = "memory_entry_id", type = IdType.INPUT)
    private Long memoryEntryId;

    @TableField("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
