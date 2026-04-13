package cn.lysoy.jingu3.workspace.security;

import java.nio.file.Path;

/**
 * 将相对路径解析到工作空间根下，防止越权访问（目录遍历）。
 */
public final class PathValidator {

    private PathValidator() {
    }

    /**
     * @param workspaceRoot 用户工作空间根（已规范化绝对路径）
     * @param relativePath 相对路径，允许为空表示根目录
     * @return 解析后的绝对路径，保证位于 {@code workspaceRoot} 之下
     */
    public static Path resolveUnderRoot(Path workspaceRoot, String relativePath) {
        Path root = workspaceRoot.toAbsolutePath().normalize();
        String rel = relativePath == null || relativePath.isBlank() ? "." : relativePath.trim();
        Path candidate = root.resolve(rel).normalize();
        if (!candidate.startsWith(root)) {
            throw new SecurityException("路径越出工作空间: " + rel);
        }
        return candidate;
    }
}
