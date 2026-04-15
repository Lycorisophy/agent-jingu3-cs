package cn.lysoy.jingu3.dst;

import cn.lysoy.jingu3.common.util.UtcTime;
import cn.lysoy.jingu3.dst.dto.DialogueStateVo;
import cn.lysoy.jingu3.dst.dto.PatchDialogueStateRequest;
import cn.lysoy.jingu3.dst.entity.DialogueStateEntity;
import cn.lysoy.jingu3.dst.mapper.DialogueStateMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该会话尚无状态");
        }
        return DialogueStateVo.from(row);
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
            throw new ResponseStatusException(HttpStatus.CONFLICT, "revision 不匹配，请刷新后重试");
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该会话尚无状态");
        }
        var now = UtcTime.nowLocalDateTime();
        e.setRevision(e.getRevision() + 1);
        e.setUpdatedAt(now);
        dialogueStateMapper.updateById(e);
        return DialogueStateVo.from(e);
    }
}
