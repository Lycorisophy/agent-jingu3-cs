package cn.lysoy.jingu3.service.context.dst;

import cn.lysoy.jingu3.common.constant.DstApiMessages;
import cn.lysoy.jingu3.util.UtcTime;
import cn.lysoy.jingu3.common.dto.dst.PatchDialogueStateRequest;
import cn.lysoy.jingu3.common.vo.dst.DialogueStateVo;
import cn.lysoy.jingu3.mapper.dst.DialogueStateEntity;
import cn.lysoy.jingu3.mapper.dst.DialogueStateMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

@Service
public class DialogueStateService {

    private final DialogueStateMapper dialogueStateMapper;

    public DialogueStateService(DialogueStateMapper dialogueStateMapper) {
        this.dialogueStateMapper = dialogueStateMapper;
    }

    @Transactional(readOnly = true)
    public DialogueStateVo get(String conversationId) {
        String cid = conversationId.trim();
        DialogueStateEntity row = dialogueStateMapper.selectByConversationId(cid);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DstApiMessages.NO_STATE_FOR_CONVERSATION);
        }
        return DialogueStateVo.from(row);
    }

    /**
     * 供 STM 等横切能力可选读取 DST；无记录时返回 empty（不抛 404）。
     */
    @Transactional(readOnly = true)
    public Optional<DialogueStateVo> findOptional(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return Optional.empty();
        }
        DialogueStateEntity row = dialogueStateMapper.selectByConversationId(conversationId.trim());
        return row == null ? Optional.empty() : Optional.of(DialogueStateVo.from(row));
    }

    @Transactional
    public DialogueStateVo patch(String conversationId, PatchDialogueStateRequest request) {
        String cid = conversationId.trim();
        var now = UtcTime.nowLocalDateTime();
        DialogueStateEntity e = dialogueStateMapper.selectByConversationId(cid);
        if (e == null) {
            e = new DialogueStateEntity();
            e.setConversationId(cid);
            e.setSchemaVersion("1");
            e.setRevision(0L);
        }
        if (request.getExpectedRevision() != null
                && !Objects.equals(request.getExpectedRevision(), e.getRevision())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DstApiMessages.REVISION_CONFLICT);
        }
        e.setStateJson(request.getStateJson());
        e.setRevision(e.getRevision() + 1);
        e.setUpdatedAt(now);
        if (e.getId() == null) {
            dialogueStateMapper.insert(e);
        } else {
            dialogueStateMapper.updateById(e);
        }
        return DialogueStateVo.from(e);
    }

    /** 用户定稿占位：revision+1，便于客户端感知确认事件 */
    @Transactional
    public DialogueStateVo confirm(String conversationId) {
        String cid = conversationId.trim();
        DialogueStateEntity e = dialogueStateMapper.selectByConversationId(cid);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, DstApiMessages.NO_STATE_FOR_CONVERSATION);
        }
        var now = UtcTime.nowLocalDateTime();
        e.setRevision(e.getRevision() + 1);
        e.setUpdatedAt(now);
        dialogueStateMapper.updateById(e);
        return DialogueStateVo.from(e);
    }
}
