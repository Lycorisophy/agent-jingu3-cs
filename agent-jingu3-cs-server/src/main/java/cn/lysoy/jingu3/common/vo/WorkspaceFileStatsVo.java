package cn.lysoy.jingu3.common.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 工作空间磁盘占用统计（与 {@link cn.lysoy.jingu3.skill.workspace.WorkspaceStats} 对齐的 REST 视图）。
 */
@Getter
@Setter
public class WorkspaceFileStatsVo {

    private long quotaMb;

    private long fileCount;

    private long totalSizeBytes;
}
