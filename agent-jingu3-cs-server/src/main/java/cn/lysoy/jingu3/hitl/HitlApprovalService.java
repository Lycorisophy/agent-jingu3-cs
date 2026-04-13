package cn.lysoy.jingu3.hitl;

import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.hitl.dto.CreateHitlApprovalRequest;
import cn.lysoy.jingu3.hitl.dto.HitlApprovalVo;
import cn.lysoy.jingu3.hitl.entity.HitlApprovalEntity;
import cn.lysoy.jingu3.hitl.repo.HitlApprovalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class HitlApprovalService {

    private final HitlApprovalRepository repository;
    private final UserConstants userConstants;

    public HitlApprovalService(HitlApprovalRepository repository, UserConstants userConstants) {
        this.repository = repository;
        this.userConstants = userConstants;
    }

    @Transactional
    public HitlApprovalVo create(CreateHitlApprovalRequest request) {
        Instant now = Instant.now();
        HitlApprovalEntity e = new HitlApprovalEntity();
        e.setConversationId(request.getConversationId().trim());
        e.setRunId(request.getRunId() != null && !request.getRunId().isBlank() ? request.getRunId().trim() : null);
        e.setStatus(HitlApprovalStatus.PENDING);
        e.setPayloadJson(request.getPayloadJson());
        e.setCreatedAt(now);
        return HitlApprovalVo.from(repository.save(e));
    }

    @Transactional(readOnly = true)
    public List<HitlApprovalVo> listPending(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId 必填");
        }
        return repository
                .findByStatusAndConversationIdOrderByCreatedAtAsc(
                        HitlApprovalStatus.PENDING, conversationId.trim())
                .stream()
                .map(HitlApprovalVo::from)
                .toList();
    }

    @Transactional
    public HitlApprovalVo approve(long id) {
        return resolve(id, HitlApprovalStatus.APPROVED);
    }

    @Transactional
    public HitlApprovalVo reject(long id) {
        return resolve(id, HitlApprovalStatus.REJECTED);
    }

    private HitlApprovalVo resolve(long id, HitlApprovalStatus target) {
        HitlApprovalEntity e = repository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "审批单不存在"));
        if (e.getStatus() != HitlApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "审批单已处理");
        }
        Instant now = Instant.now();
        e.setStatus(target);
        e.setResolvedAt(now);
        e.setResolverUserId(userConstants.getId());
        return HitlApprovalVo.from(repository.save(e));
    }
}
