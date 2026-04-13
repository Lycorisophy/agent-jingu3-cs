package cn.lysoy.jingu3.workspace;

import java.io.IOException;
import java.util.List;

/**
 * 用户隔离目录内的安全文件操作（v0.7 Workspace Phase 1 子集）。
 */
public interface WorkspaceFileService {

    String readFile(String userId, String relativePath) throws IOException;

    void writeFile(String userId, String relativePath, String content) throws IOException;

    /** 列出目录下直接子项名称（文件或子目录名） */
    List<String> listDirectory(String userId, String relativePath) throws IOException;
}
