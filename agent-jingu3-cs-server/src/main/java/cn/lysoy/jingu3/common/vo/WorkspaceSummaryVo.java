package cn.lysoy.jingu3.common.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 工作空间概览（Phase 3 REST）。
 */
@Getter
@Setter
public class WorkspaceSummaryVo {

    private String workspaceId;

    private String userId;

    private String rootPath;

    private long quotaMb;

    private long fileCount;

    private long totalSizeBytes;
}
