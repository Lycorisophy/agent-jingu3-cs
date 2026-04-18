package cn.lysoy.jingu3.controller.api;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.vo.WorkspaceExecutionItemVo;
import cn.lysoy.jingu3.common.vo.WorkspaceFileStatsVo;
import cn.lysoy.jingu3.common.vo.WorkspaceSummaryVo;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.service.workspace.WorkspaceManagementService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * v0.7 Workspace Phase 3：工作空间管理/统计与执行历史查询（单用户阶段取 {@link UserConstants#getId()}）。
 */
@RestController
@RequestMapping("/api/v1/workspace")
@ConditionalOnProperty(
        prefix = "jingu3.workspace",
        name = {"enabled", "rest-api-enabled"},
        havingValue = "true",
        matchIfMissing = true)
public class WorkspaceController {

    private final WorkspaceManagementService workspaceManagementService;

    private final UserConstants userConstants;

    public WorkspaceController(WorkspaceManagementService workspaceManagementService, UserConstants userConstants) {
        this.workspaceManagementService = workspaceManagementService;
        this.userConstants = userConstants;
    }

    @GetMapping
    public ApiResult<WorkspaceSummaryVo> summary() {
        return ApiResult.ok(workspaceManagementService.getSummary(userConstants.getId()));
    }

    @GetMapping("/stats")
    public ApiResult<WorkspaceFileStatsVo> stats() {
        return ApiResult.ok(workspaceManagementService.getFileStats(userConstants.getId()));
    }

    @PostMapping("/reset")
    public ApiResult<Void> reset() {
        workspaceManagementService.resetWorkspace(userConstants.getId());
        return ApiResult.ok(null);
    }

    @DeleteMapping
    public ApiResult<Void> delete() {
        workspaceManagementService.deleteWorkspace(userConstants.getId());
        return ApiResult.ok(null);
    }

    @GetMapping("/executions")
    public ApiResult<List<WorkspaceExecutionItemVo>> executions(@RequestParam(value = "limit", required = false) Integer limit) {
        return ApiResult.ok(workspaceManagementService.listExecutions(userConstants.getId(), limit));
    }
}
