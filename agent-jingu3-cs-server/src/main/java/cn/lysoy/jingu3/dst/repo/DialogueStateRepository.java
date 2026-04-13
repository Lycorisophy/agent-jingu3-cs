package cn.lysoy.jingu3.dst.repo;

import cn.lysoy.jingu3.dst.entity.DialogueStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DialogueStateRepository extends JpaRepository<DialogueStateEntity, Long> {

    Optional<DialogueStateEntity> findByConversationId(String conversationId);
}
