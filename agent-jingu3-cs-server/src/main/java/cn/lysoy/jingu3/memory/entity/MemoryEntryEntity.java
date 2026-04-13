package cn.lysoy.jingu3.memory.entity;

import cn.lysoy.jingu3.memory.MemoryEntryKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "memory_entry")
@Getter
@Setter
public class MemoryEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private MemoryEntryKind kind;

    @Column(length = 512)
    private String summary;

    @Lob
    @Column(name = "body")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
