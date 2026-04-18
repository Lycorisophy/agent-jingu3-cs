package cn.lysoy.jingu3.service.workspace;

import cn.lysoy.jingu3.skill.workspace.entity.WorkspaceEntity;

import java.io.IOException;
import java.util.Optional;

/**
 * 工作空间元数据行（{@code workspace} 表）与磁盘根目录对齐。
 */
public interface WorkspaceMetadataService {

    /**
     * 若不存在则插入一行并返回主键；已存在则返回已有 id。
     */
    String ensureWorkspaceRow(String userId) throws IOException;

    Optional<WorkspaceEntity> findRow(String userId);

    /**
     * 按用户删除元数据（级联删除执行历史）。
     */
    void deleteMetadataByUserId(String userId);
}
