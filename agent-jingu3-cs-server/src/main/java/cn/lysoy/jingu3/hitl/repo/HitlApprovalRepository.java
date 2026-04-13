package cn.lysoy.jingu3.hitl.repo;

import cn.lysoy.jingu3.hitl.HitlApprovalStatus;
import cn.lysoy.jingu3.hitl.entity.HitlApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HitlApprovalRepository extends JpaRepository<HitlApprovalEntity, Long> {

    List<HitlApprovalEntity> findByStatusAndConversationIdOrderByCreatedAtAsc(
            HitlApprovalStatus status, String conversationId);
}
