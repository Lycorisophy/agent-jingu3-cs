package cn.lysoy.jingu3.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 与 {@link MemoryEntryEntity} 中 kind=FACT 的条目关联的可选元数据（M1 草案）。
 */
@TableName("fact_metadata")
@Getter
@Setter
public class FactMetadataEntity {

    @TableId(value = "memory_entry_id", type = IdType.INPUT)
    private Long memoryEntryId;

    private String tag;

    @TableField("temporal_tier")
    private String temporalTier;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;
}
