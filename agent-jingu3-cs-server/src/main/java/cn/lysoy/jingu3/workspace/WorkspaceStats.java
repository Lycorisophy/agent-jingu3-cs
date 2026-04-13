package cn.lysoy.jingu3.workspace;

import lombok.Data;

/**
 * 工作空间用量统计（配额为配置占位，Phase 3 可做硬 enforcement）。
 */
@Data
public class WorkspaceStats {

    private long fileCount;

    private long totalSizeBytes;

    private long quotaMb;
}
