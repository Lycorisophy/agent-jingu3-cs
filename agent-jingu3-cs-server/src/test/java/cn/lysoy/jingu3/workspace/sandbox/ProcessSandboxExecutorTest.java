package cn.lysoy.jingu3.skill.workspace.sandbox;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.skill.workspace.DefaultWorkspaceManager;
import cn.lysoy.jingu3.skill.workspace.service.WorkspaceExecutionRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ProcessSandboxExecutorTest {

    @Test
    void executeNullCode(@TempDir Path tmp) {
        ProcessSandboxExecutor ex = executor(tmp);
        SandboxResult r = ex.execute("u1", "python", null, 10);
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorType()).isEqualTo("code_null");
    }

    @Test
    void executeUnsupportedLanguage(@TempDir Path tmp) {
        ProcessSandboxExecutor ex = executor(tmp);
        SandboxResult r = ex.execute("u1", "ruby", "1", 10);
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorType()).isEqualTo("unsupported_language");
    }

    @Test
    void executeCodeTooLong(@TempDir Path tmp) {
        Jingu3Properties p = props(tmp);
        p.getWorkspace().getSandbox().setMaxCodeChars(4);
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        ProcessSandboxExecutor ex = new ProcessSandboxExecutor(mgr, p, noRecorder());
        SandboxResult r = ex.execute("u1", "python", "12345", 10);
        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorType()).isEqualTo("code_too_long");
    }

    @Test
    void executePythonPrintsWhenPythonOnPath(@TempDir Path tmp) throws Exception {
        assumePythonRunnable();
        ProcessSandboxExecutor ex = executor(tmp);
        SandboxResult r = ex.execute("u1", "python", "print(42)", 30);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getStdout().trim()).isEqualTo("42");
        assertThat(r.getExitCode()).isZero();
    }

    @Test
    void executeFileReadsWorkspaceScript(@TempDir Path tmp) throws Exception {
        assumePythonRunnable();
        Path userRoot = tmp.resolve("u1");
        Files.createDirectories(userRoot);
        Files.writeString(userRoot.resolve("hello.py"), "print('hi')");
        Jingu3Properties p = props(tmp);
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        ProcessSandboxExecutor ex = new ProcessSandboxExecutor(mgr, p, noRecorder());
        SandboxResult r = ex.executeFile("u1", "python", "hello.py", 30);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getStdout().trim()).isEqualTo("hi");
    }

    private static ProcessSandboxExecutor executor(Path workspaceRoot) {
        Jingu3Properties p = props(workspaceRoot);
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        return new ProcessSandboxExecutor(mgr, p, noRecorder());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<WorkspaceExecutionRecorder> noRecorder() {
        ObjectProvider<WorkspaceExecutionRecorder> p = Mockito.mock(ObjectProvider.class);
        Mockito.doNothing().when(p).ifAvailable(Mockito.any());
        return p;
    }

    private static Jingu3Properties props(Path workspaceRoot) {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(workspaceRoot.toString());
        p.getWorkspace().getSandbox().setEnabled(true);
        return p;
    }

    private static void assumePythonRunnable() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("python", "-c", "print(1)");
        Process proc = pb.start();
        assumeTrue(proc.waitFor(15, TimeUnit.SECONDS));
        assumeTrue(proc.exitValue() == 0);
    }
}
