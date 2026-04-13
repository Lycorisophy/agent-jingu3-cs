package cn.lysoy.jingu3.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

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
}
