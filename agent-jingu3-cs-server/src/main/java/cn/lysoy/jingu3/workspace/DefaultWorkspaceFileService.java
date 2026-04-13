package cn.lysoy.jingu3.workspace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.workspace.security.PathValidator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class DefaultWorkspaceFileService implements WorkspaceFileService {

    private final Jingu3Properties properties;

    public DefaultWorkspaceFileService(Jingu3Properties properties) {
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
        Files.createDirectories(file.getParent());
        Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
    }

    @Override
    public List<String> listDirectory(String userId, String relativePath) throws IOException {
        Path dir = PathValidator.resolveUnderRoot(userRoot(userId), relativePath);
        if (!Files.isDirectory(dir)) {
            throw new IOException("不是目录或不存在: " + relativePath);
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).sorted(Comparator.naturalOrder()).toList();
        }
    }

    private Path userRoot(String userId) throws IOException {
        if (userId == null || userId.isBlank()) {
            throw new IOException("userId 不能为空");
        }
        String base = properties.getWorkspace().getRootDir();
        Path root = Path.of(base).toAbsolutePath().normalize();
        Path u = root.resolve(userId.trim()).normalize();
        if (!u.startsWith(root)) {
            throw new SecurityException("非法 userId");
        }
        Files.createDirectories(u);
        return u;
    }
}
