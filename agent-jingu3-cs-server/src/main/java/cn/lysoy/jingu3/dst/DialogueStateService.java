package cn.lysoy.jingu3.dst;

import cn.lysoy.jingu3.dst.dto.DialogueStateVo;
import cn.lysoy.jingu3.dst.dto.PatchDialogueStateRequest;
import cn.lysoy.jingu3.dst.entity.DialogueStateEntity;
import cn.lysoy.jingu3.dst.repo.DialogueStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class DialogueStateService {

    private final DialogueStateRepository repository;

    public DialogueStateService(DialogueStateRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public DialogueStateVo get(String conversationId) {
        String cid = conversationId.trim();
        return repository
                .findByConversationId(cid)
                .map(DialogueStateVo::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该会话尚无状态"));
    }

    @Transactional
    public DialogueStateVo patch(String conversationId, PatchDialogueStateRequest request) {
        String cid = conversationId.trim();
        Instant now = Instant.now();
        DialogueStateEntity e = repository.findByConversationId(cid).orElseGet(DialogueStateEntity::new);
        if (e.getId() == null) {
            e.setConversationId(cid);
            e.setSchemaVersion("1");
            e.setRevision(0L);
        }
        if (request.getExpectedRevision() != null && request.getExpectedRevision() != e.getRevision()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "revision 不匹配，请刷新后重试");
        }
        e.setStateJson(request.getStateJson());
        e.setRevision(e.getRevision() + 1);
        e.setUpdatedAt(now);
        return DialogueStateVo.from(repository.save(e));
    }

    /** 用户定稿占位：revision+1，便于客户端感知确认事件 */
    @Transactional
    public DialogueStateVo confirm(String conversationId) {
        String cid = conversationId.trim();
        DialogueStateEntity e = repository
                .findByConversationId(cid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该会话尚无状态"));
        Instant now = Instant.now();
        e.setRevision(e.getRevision() + 1);
        e.setUpdatedAt(now);
        return DialogueStateVo.from(repository.save(e));
    }
}
