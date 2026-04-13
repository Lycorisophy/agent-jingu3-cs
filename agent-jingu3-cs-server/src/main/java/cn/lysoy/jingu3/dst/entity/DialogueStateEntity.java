package cn.lysoy.jingu3.dst.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "dialogue_state")
@Getter
@Setter
public class DialogueStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, unique = true, length = 128)
    private String conversationId;

    @Column(name = "schema_version", nullable = false, length = 32)
    private String schemaVersion = "1";

    @Lob
    @Column(name = "state_json", nullable = false)
    private String stateJson;

    @Column(name = "revision", nullable = false)
    private long revision = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
