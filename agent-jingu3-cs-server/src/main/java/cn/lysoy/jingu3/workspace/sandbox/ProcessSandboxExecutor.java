package cn.lysoy.jingu3.workspace.sandbox;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.workspace.WorkspaceManager;
import cn.lysoy.jingu3.workspace.security.PathValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 {@link ProcessBuilder} 的本地进程沙箱；工作目录为用户工作空间根，脚本写入 {@code .sandbox/runs/}。
 */
@Slf4j
@Service
@ConditionalOnBean(WorkspaceManager.class)
@ConditionalOnProperty(prefix = "jingu3.workspace.sandbox", name = "enabled", havingValue = "true")
public class ProcessSandboxExecutor implements SandboxExecutor {

    private static final String RUNS_DIR = ".sandbox/runs";

    private final WorkspaceManager workspaceManager;

    private final Jingu3Properties properties;

    public ProcessSandboxExecutor(WorkspaceManager workspaceManager, Jingu3Properties properties) {
        this.workspaceManager = workspaceManager;
        this.properties = properties;
    }

    @Override
    public SandboxResult execute(String userId, String language, String code, int timeoutSeconds) {
        if (code == null) {
            return fail("code_null", "code 不能为空", 0);
        }
        Jingu3Properties.Workspace.Sandbox cfg = properties.getWorkspace().getSandbox();
        if (code.length() > cfg.getMaxCodeChars()) {
            return fail("code_too_long", "代码超过 maxCodeChars", 0);
        }
        int timeout = clampTimeout(timeoutSeconds, cfg.getMaxTimeoutSeconds());
        String lang = normalizeLanguage(language);
        try {
            Path userRoot = workspaceManager.resolveUserRoot(userId);
            Path runs = PathValidator.resolveUnderRoot(userRoot, RUNS_DIR);
            Files.createDirectories(runs);
            String id = UUID.randomUUID().toString().replace("-", "");
            Path script;
            ProcessBuilder pb;
            if ("python".equals(lang)) {
                script = runs.resolve("run_" + id + ".py");
                Files.writeString(script, code, StandardCharsets.UTF_8);
                pb = new ProcessBuilder(cfg.getPythonCommand(), script.toAbsolutePath().toString());
            } else if ("javascript".equals(lang)) {
                script = runs.resolve("run_" + id + ".mjs");
                Files.writeString(script, code, StandardCharsets.UTF_8);
                pb = new ProcessBuilder(cfg.getNodeCommand(), script.toAbsolutePath().toString());
            } else {
                return fail("unsupported_language", "仅支持 python、javascript", 0);
            }
            pb.directory(userRoot.toFile());
            pb.redirectErrorStream(true);
            return runProcess(pb, timeout, cfg.getMaxOutputChars(), script);
        } catch (Exception e) {
            log.warn("sandbox execute failed: {}", e.toString());
            return fail("execute_error", e.getMessage(), 0);
        }
    }

    @Override
    public SandboxResult executeFile(String userId, String language, String relativePath, int timeoutSeconds) {
        Jingu3Properties.Workspace.Sandbox cfg = properties.getWorkspace().getSandbox();
        int timeout = clampTimeout(timeoutSeconds, cfg.getMaxTimeoutSeconds());
        String lang = normalizeLanguage(language);
        try {
            Path userRoot = workspaceManager.resolveUserRoot(userId);
            Path file = PathValidator.resolveUnderRoot(userRoot, relativePath);
            if (!Files.isRegularFile(file)) {
                return fail("not_a_file", "不是文件或不存在: " + relativePath, 0);
            }
            ProcessBuilder pb;
            if ("python".equals(lang)) {
                pb = new ProcessBuilder(cfg.getPythonCommand(), file.toAbsolutePath().toString());
            } else if ("javascript".equals(lang)) {
                pb = new ProcessBuilder(cfg.getNodeCommand(), file.toAbsolutePath().toString());
            } else {
                return fail("unsupported_language", "仅支持 python、javascript", 0);
            }
            pb.directory(userRoot.toFile());
            pb.redirectErrorStream(true);
            return runProcess(pb, timeout, cfg.getMaxOutputChars(), null);
        } catch (Exception e) {
            log.warn("sandbox executeFile failed: {}", e.toString());
            return fail("execute_error", e.getMessage(), 0);
        }
    }

    private static SandboxResult fail(String errorType, String message, long ms) {
        return SandboxResult.builder()
                .success(false)
                .stdout("")
                .stderr(message)
                .exitCode(-1)
                .executionTimeMs(ms)
                .errorType(errorType)
                .timeout(false)
                .build();
    }

    private static int clampTimeout(int requested, int max) {
        if (requested <= 0) {
            return max;
        }
        return Math.min(requested, max);
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "";
        }
        String l = language.trim().toLowerCase(Locale.ROOT);
        if ("js".equals(l) || "node".equals(l)) {
            return "javascript";
        }
        return l;
    }

    private SandboxResult runProcess(ProcessBuilder pb, int timeoutSec, int maxOutChars, Path scriptToDelete) {
        long t0 = System.nanoTime();
        Process p = null;
        try {
            p = pb.start();
            boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            if (!finished) {
                p.destroyForcibly();
                return SandboxResult.builder()
                        .success(false)
                        .stdout("")
                        .stderr("执行超时（" + timeoutSec + "s）")
                        .exitCode(-1)
                        .executionTimeMs(ms)
                        .errorType("timeout")
                        .timeout(true)
                        .build();
            }
            String out = readLimited(p.getInputStream(), maxOutChars);
            int code = p.exitValue();
            return SandboxResult.builder()
                    .success(code == 0)
                    .stdout(out)
                    .stderr("")
                    .exitCode(code)
                    .executionTimeMs(ms)
                    .errorType(code == 0 ? null : "non_zero_exit")
                    .timeout(false)
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            if (p != null) {
                p.destroyForcibly();
            }
            return fail("interrupted", e.getMessage(), ms);
        } catch (IOException e) {
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            return fail("io", e.getMessage(), ms);
        } finally {
            if (scriptToDelete != null) {
                try {
                    Files.deleteIfExists(scriptToDelete);
                } catch (IOException ignored) {
                    log.debug("delete temp script: {}", scriptToDelete);
                }
            }
        }
    }

    private static String readLimited(InputStream in, int maxChars) throws IOException {
        byte[] buf = in.readAllBytes();
        String s = new String(buf, StandardCharsets.UTF_8);
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "\n...[truncated]";
    }
}
