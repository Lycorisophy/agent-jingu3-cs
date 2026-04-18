package cn.lysoy.jingu3.skill.workspace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(prefix = "jingu3.workspace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultWorkspaceManager implements WorkspaceManager {

    private final Jingu3Properties properties;

    public DefaultWorkspaceManager(Jingu3Properties properties) {
        this.properties = properties;
    }

    @Override
    public Path resolveUserRoot(String userId) throws IOException {
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

    @Override
    public Workspace getOrCreateWorkspace(String userId) throws IOException {
        Path dir = resolveUserRoot(userId);
        LocalDateTime created = directoryCreateTime(dir);
        LocalDateTime now = LocalDateTime.now();
        long quota = properties.getWorkspace().getDefaultQuotaMb();
        return Workspace.builder()
                .id(userId.trim())
                .userId(userId.trim())
                .rootPath(dir.toString())
                .name(userId.trim())
                .createdAt(created)
                .lastActiveAt(now)
                .status("ACTIVE")
                .quotaMb(quota)
                .build();
    }

    @Override
    public Optional<Workspace> getWorkspace(String userId) throws IOException {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        String base = properties.getWorkspace().getRootDir();
        Path root = Path.of(base).toAbsolutePath().normalize();
        Path u = root.resolve(userId.trim()).normalize();
        if (!u.startsWith(root)) {
            throw new SecurityException("非法 userId");
        }
        if (!Files.isDirectory(u, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        LocalDateTime created = directoryCreateTime(u);
        long quota = properties.getWorkspace().getDefaultQuotaMb();
        return Optional.of(Workspace.builder()
                .id(userId.trim())
                .userId(userId.trim())
                .rootPath(u.toString())
                .name(userId.trim())
                .createdAt(created)
                .lastActiveAt(LocalDateTime.now())
                .status("ACTIVE")
                .quotaMb(quota)
                .build());
    }

    @Override
    public void resetWorkspace(String userId) throws IOException {
        Path dir = resolveUserRoot(userId);
        deleteTreeContentsOnly(dir);
    }

    @Override
    public void deleteWorkspace(String userId) throws IOException {
        if (userId == null || userId.isBlank()) {
            throw new IOException("userId 不能为空");
        }
        String base = properties.getWorkspace().getRootDir();
        Path root = Path.of(base).toAbsolutePath().normalize();
        Path u = root.resolve(userId.trim()).normalize();
        if (!u.startsWith(root)) {
            throw new SecurityException("非法 userId");
        }
        if (Files.exists(u, LinkOption.NOFOLLOW_LINKS)) {
            deleteRecursively(u);
        }
    }

    @Override
    public WorkspaceStats getStats(String userId) throws IOException {
        Path dir = resolveUserRoot(userId);
        long fileCount = 0;
        long totalBytes = 0;
        if (Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> stream = Files.walk(dir)) {
                for (Path p : stream.filter(Files::isRegularFile).toList()) {
                    fileCount++;
                    totalBytes += Files.size(p);
                }
            }
        }
        WorkspaceStats stats = new WorkspaceStats();
        stats.setFileCount(fileCount);
        stats.setTotalSizeBytes(totalBytes);
        stats.setQuotaMb(properties.getWorkspace().getDefaultQuotaMb());
        return stats;
    }

    private static void deleteTreeContentsOnly(Path dir) throws IOException {
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path child : stream.toList()) {
                deleteRecursively(child);
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> inner = Files.list(path)) {
                for (Path c : inner.toList()) {
                    deleteRecursively(c);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private static LocalDateTime directoryCreateTime(Path dir) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(dir, BasicFileAttributes.class);
        Instant instant = attrs.creationTime().toInstant();
        if (instant.toEpochMilli() == 0) {
            instant = attrs.lastModifiedTime().toInstant();
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
