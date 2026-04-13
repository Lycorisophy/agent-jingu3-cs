package cn.lysoy.jingu3.memory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 与 {@link MemoryEntryEntity} 中 kind=FACT 的条目关联的可选元数据（M1 草案）。
 */
@Entity
@Table(name = "fact_metadata")
@Getter
@Setter
public class FactMetadataEntity {

    @Id
    @Column(name = "memory_entry_id")
    private Long memoryEntryId;

    @Column(length = 128)
    private String tag;
}
