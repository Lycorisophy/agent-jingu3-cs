package cn.lysoy.jingu3.service.workspace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.workspace.WorkspaceManager;
import cn.lysoy.jingu3.workspace.WorkspaceStats;
import cn.lysoy.jingu3.workspace.security.PathValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(prefix = "jingu3.workspace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultWorkspaceFileService implements WorkspaceFileService {

    private final WorkspaceManager workspaceManager;

    private final Jingu3Properties properties;

    public DefaultWorkspaceFileService(WorkspaceManager workspaceManager, Jingu3Properties properties) {
        this.workspaceManager = workspaceManager;
        this.properties = properties;
    }

    @Override
    public String readFile(String userId, String relativePath) throws IOException {
        Path file = PathValidator.resolveUnderRoot(userRoot(userId), relativePath);
        if (!Files.isRegularFile(file)) {
            throw new IOException("不是文件或不存在: " + relativePath);
        }
        long maxBytes = properties.getWorkspace().getMaxFileSizeMb() * 1024L * 1024L;
        long size = Files.size(file);
        if (size > maxBytes) {
            throw new IOException("文件超过大小限制 " + properties.getWorkspace().getMaxFileSizeMb() + "MB");
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Override
    public void writeFile(String userId, String relativePath, String content) throws IOException {
        Path file = PathValidator.resolveUnderRoot(userRoot(userId), relativePath);
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        long maxBytes = properties.getWorkspace().getMaxFileSizeMb() * 1024L * 1024L;
        if (bytes.length > maxBytes) {
            throw new IOException("内容超过大小限制 " + properties.getWorkspace().getMaxFileSizeMb() + "MB");
        }
        long quotaMb = properties.getWorkspace().getDefaultQuotaMb();
        if (quotaMb > 0) {
            long quotaBytes = quotaMb * 1024L * 1024L;
            WorkspaceStats st = workspaceManager.getStats(userId);
            long oldSize = Files.isRegularFile(file) ? Files.size(file) : 0L;
            long newTotal = st.getTotalSizeBytes() - oldSize + bytes.length;
            if (newTotal > quotaBytes) {
                throw new IOException("超过工作空间配额（" + quotaMb + "MB）");
            }
        }
        Path parent = file.getParent();
        if (parent == null) {
            throw new IOException("无法解析父目录: " + relativePath);
        }
        Files.createDirectories(parent);
        Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
    }

    @Override
    public List<String> listDirectory(String userId, String relativePath) throws IOException {
        Path dir = PathValidator.resolveUnderRoot(userRoot(userId), relativePath);
        if (!Files.isDirectory(dir)) {
            throw new IOException("不是目录或不存在: " + relativePath);
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> {
                        Path fn = p.getFileName();
                        return fn != null ? fn.toString() : "";
                    })
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    private Path userRoot(String userId) throws IOException {
        return workspaceManager.resolveUserRoot(userId);
    }
}
