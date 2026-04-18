package cn.lysoy.jingu3.controller.api;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.service.context.dst.DialogueStateService;
import cn.lysoy.jingu3.common.dto.dst.PatchDialogueStateRequest;
import cn.lysoy.jingu3.common.vo.dst.DialogueStateVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DST 占位：按 {@code conversationId} 读写侧栏状态（对齐极简设计 §4）。
 */
@RestController
@RequestMapping("/api/v1/dst")
public class DialogueStateController {

    private final DialogueStateService dialogueStateService;

    public DialogueStateController(DialogueStateService dialogueStateService) {
        this.dialogueStateService = dialogueStateService;
    }

    @GetMapping("/{conversationId}")
    public ApiResult<DialogueStateVo> get(@PathVariable("conversationId") String conversationId) {
        return ApiResult.ok(dialogueStateService.get(conversationId));
    }

    @PatchMapping("/{conversationId}")
    public ApiResult<DialogueStateVo> patch(
            @PathVariable("conversationId") String conversationId,
            @Valid @RequestBody PatchDialogueStateRequest request) {
        return ApiResult.ok(dialogueStateService.patch(conversationId, request));
    }

    @PostMapping("/{conversationId}/confirm")
    public ApiResult<DialogueStateVo> confirm(@PathVariable("conversationId") String conversationId) {
        return ApiResult.ok(dialogueStateService.confirm(conversationId));
    }
}
