package cn.lysoy.jingu3.workspace.service;

import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.common.vo.WorkspaceExecutionItemVo;
import cn.lysoy.jingu3.common.vo.WorkspaceFileStatsVo;
import cn.lysoy.jingu3.common.vo.WorkspaceSummaryVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.workspace.WorkspaceManager;
import cn.lysoy.jingu3.workspace.WorkspaceStats;
import cn.lysoy.jingu3.workspace.entity.WorkspaceEntity;
import cn.lysoy.jingu3.workspace.entity.WorkspaceExecutionEntity;
import cn.lysoy.jingu3.workspace.mapper.WorkspaceExecutionMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "jingu3.workspace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WorkspaceManagementService {

    private final WorkspaceManager workspaceManager;

    private final WorkspaceMetadataService workspaceMetadataService;

    private final WorkspaceExecutionMapper workspaceExecutionMapper;

    private final Jingu3Properties properties;

    public WorkspaceManagementService(
            WorkspaceManager workspaceManager,
            WorkspaceMetadataService workspaceMetadataService,
            WorkspaceExecutionMapper workspaceExecutionMapper,
            Jingu3Properties properties) {
        this.workspaceManager = workspaceManager;
        this.workspaceMetadataService = workspaceMetadataService;
        this.workspaceExecutionMapper = workspaceExecutionMapper;
        this.properties = properties;
    }

    public WorkspaceSummaryVo getSummary(String userId) {
        try {
            workspaceManager.getOrCreateWorkspace(userId);
            String wid = workspaceMetadataService.ensureWorkspaceRow(userId);
            WorkspaceStats st = workspaceManager.getStats(userId);
            Optional<WorkspaceEntity> row = workspaceMetadataService.findRow(userId);
            long quota = row.map(WorkspaceEntity::getQuotaMb).orElse(properties.getWorkspace().getDefaultQuotaMb());

            WorkspaceSummaryVo vo = new WorkspaceSummaryVo();
            vo.setWorkspaceId(wid);
            vo.setUserId(userId);
            vo.setRootPath(workspaceManager.resolveUserRoot(userId).toString());
            vo.setQuotaMb(quota);
            vo.setFileCount(st.getFileCount());
            vo.setTotalSizeBytes(st.getTotalSizeBytes());
            return vo;
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, e.getMessage(), e);
        }
    }

    public WorkspaceFileStatsVo getFileStats(String userId) {
        try {
            workspaceManager.getOrCreateWorkspace(userId);
            workspaceMetadataService.ensureWorkspaceRow(userId);
            WorkspaceStats st = workspaceManager.getStats(userId);
            Optional<WorkspaceEntity> row = workspaceMetadataService.findRow(userId);
            long quota = row.map(WorkspaceEntity::getQuotaMb).orElse(properties.getWorkspace().getDefaultQuotaMb());
            WorkspaceFileStatsVo vo = new WorkspaceFileStatsVo();
            vo.setQuotaMb(quota);
            vo.setFileCount(st.getFileCount());
            vo.setTotalSizeBytes(st.getTotalSizeBytes());
            return vo;
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, e.getMessage(), e);
        }
    }

    public void resetWorkspace(String userId) {
        try {
            workspaceManager.resetWorkspace(userId);
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, e.getMessage(), e);
        }
    }

    public void deleteWorkspace(String userId) {
        try {
            workspaceManager.deleteWorkspace(userId);
            workspaceMetadataService.deleteMetadataByUserId(userId);
        } catch (IOException e) {
            throw new ServiceException(ErrorCode.BAD_REQUEST, e.getMessage(), e);
        }
    }

    public List<WorkspaceExecutionItemVo> listExecutions(String userId, Integer limitParam) {
        if (!properties.getWorkspace().isExecutionHistoryEnabled()) {
            return List.of();
        }
        int lim = limitParam == null ? 20 : limitParam;
        int cap = properties.getWorkspace().getExecutionHistoryListLimit();
        lim = Math.min(Math.max(lim, 1), Math.max(1, cap));
        List<WorkspaceExecutionEntity> rows = workspaceExecutionMapper.selectRecentByUserId(userId, lim);
        List<WorkspaceExecutionItemVo> out = new ArrayList<>(rows.size());
        for (WorkspaceExecutionEntity e : rows) {
            out.add(toExecutionVo(e));
        }
        return out;
    }

    private static WorkspaceExecutionItemVo toExecutionVo(WorkspaceExecutionEntity e) {
        WorkspaceExecutionItemVo vo = new WorkspaceExecutionItemVo();
        vo.setId(e.getId());
        vo.setLanguage(e.getLanguage());
        vo.setRunMode(e.getRunMode());
        vo.setRelativePath(e.getRelativePath());
        vo.setCodeHash(e.getCodeHash());
        vo.setSuccess(Boolean.TRUE.equals(e.getSuccess()));
        vo.setExitCode(e.getExitCode() == null ? -1 : e.getExitCode());
        vo.setDurationMs(e.getDurationMs() == null ? 0L : e.getDurationMs());
        vo.setErrorType(e.getErrorType());
        vo.setTimedOut(Boolean.TRUE.equals(e.getTimedOut()));
        vo.setCreatedAt(e.getCreatedAt() == null ? null : e.getCreatedAt().toString());
        return vo;
    }
}
