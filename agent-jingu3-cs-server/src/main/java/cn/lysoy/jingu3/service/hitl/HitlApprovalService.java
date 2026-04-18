package cn.lysoy.jingu3.service.hitl;

import cn.lysoy.jingu3.common.constant.HitlApiMessages;
import cn.lysoy.jingu3.util.UtcTime;
import cn.lysoy.jingu3.common.enums.HitlApprovalStatus;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.common.dto.hitl.CreateHitlApprovalRequest;
import cn.lysoy.jingu3.common.vo.hitl.HitlApprovalVo;
import cn.lysoy.jingu3.common.entity.HitlApprovalEntity;
import cn.lysoy.jingu3.mapper.hitl.HitlApprovalMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HitlApprovalService {

    private final HitlApprovalMapper hitlApprovalMapper;
    private final UserConstants userConstants;

    public HitlApprovalService(HitlApprovalMapper hitlApprovalMapper, UserConstants userConstants) {
        this.hitlApprovalMapper = hitlApprovalMapper;
        this.userConstants = userConstants;
    }

    @Transactional
    public HitlApprovalVo create(CreateHitlApprovalRequest request) {
        var now = UtcTime.nowLocalDateTime();
        HitlApprovalEntity e = new HitlApprovalEntity();
        e.setConversationId(request.getConversationId().trim());
        e.setRunId(request.getRunId() != null && !request.getRunId().isBlank() ? request.getRunId().trim() : null);
        e.setStatus(HitlApprovalStatus.PENDING);
        e.setPayloadJson(request.getPayloadJson());
        e.setCreatedAt(now);
        hitlApprovalMapper.insert(e);
        return HitlApprovalVo.from(e);
    }

    @Transactional(readOnly = true)
    public List<HitlApprovalVo> listPending(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, HitlApiMessages.CONVERSATION_ID_REQUIRED);
        }
        return hitlApprovalMapper
                .selectByStatusAndConversationIdOrderByCreatedAtAsc(
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
        HitlApprovalEntity e = hitlApprovalMapper.selectById(id);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, HitlApiMessages.APPROVAL_NOT_FOUND);
        }
        if (e.getStatus() != HitlApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, HitlApiMessages.APPROVAL_ALREADY_RESOLVED);
        }
        var now = UtcTime.nowLocalDateTime();
        e.setStatus(target);
        e.setResolvedAt(now);
        e.setResolverUserId(userConstants.getId());
        hitlApprovalMapper.updateById(e);
        return HitlApprovalVo.from(e);
    }
}
