package cn.lysoy.jingu3.workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 用户工作空间根目录解析与生命周期（重置、删除、统计）。
 */
public interface WorkspaceManager {

    /**
     * 解析并确保用户工作空间根目录存在。
     */
    Path resolveUserRoot(String userId) throws IOException;

    Workspace getOrCreateWorkspace(String userId) throws IOException;

    Optional<Workspace> getWorkspace(String userId) throws IOException;

    /**
     * 清空目录内全部内容，保留用户根目录本身。
     */
    void resetWorkspace(String userId) throws IOException;

    /**
     * 删除用户工作空间目录（含内容）。
     */
    void deleteWorkspace(String userId) throws IOException;

    WorkspaceStats getStats(String userId) throws IOException;
}
