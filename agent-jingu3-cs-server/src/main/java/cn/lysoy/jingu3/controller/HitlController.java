package cn.lysoy.jingu3.controller;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.hitl.HitlApprovalService;
import cn.lysoy.jingu3.hitl.dto.CreateHitlApprovalRequest;
import cn.lysoy.jingu3.hitl.dto.HitlApprovalVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HITL MVP：创建待审批、查询 pending、approve/reject（对齐极简设计 §3）。
 */
@RestController
@RequestMapping("/api/v1/hitl")
public class HitlController {

    private final HitlApprovalService hitlApprovalService;

    public HitlController(HitlApprovalService hitlApprovalService) {
        this.hitlApprovalService = hitlApprovalService;
    }

    @PostMapping("/approvals")
    public ApiResult<HitlApprovalVo> create(@Valid @RequestBody CreateHitlApprovalRequest request) {
        return ApiResult.ok(hitlApprovalService.create(request));
    }

    @GetMapping("/pending")
    public ApiResult<List<HitlApprovalVo>> pending(@RequestParam("conversationId") String conversationId) {
        return ApiResult.ok(hitlApprovalService.listPending(conversationId));
    }

    @PostMapping("/{id}/approve")
    public ApiResult<HitlApprovalVo> approve(@PathVariable("id") long id) {
        return ApiResult.ok(hitlApprovalService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ApiResult<HitlApprovalVo> reject(@PathVariable("id") long id) {
        return ApiResult.ok(hitlApprovalService.reject(id));
    }
}
