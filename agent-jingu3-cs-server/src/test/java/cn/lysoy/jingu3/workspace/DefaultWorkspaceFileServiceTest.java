package cn.lysoy.jingu3.skill.workspace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.service.workspace.DefaultWorkspaceFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultWorkspaceFileServiceTest {

    @Test
    void readWriteAndList(@TempDir Path tmp) throws Exception {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        p.getWorkspace().setMaxFileSizeMb(1);
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        DefaultWorkspaceFileService svc = new DefaultWorkspaceFileService(mgr, p);

        svc.writeFile("u1", "a/b.txt", "hello");
        assertThat(svc.readFile("u1", "a/b.txt")).isEqualTo("hello");
        assertThat(svc.listDirectory("u1", "a")).containsExactly("b.txt");
        assertThat(svc.listDirectory("u1", ".")).contains("a");
    }

    @Test
    void rejectsTraversal(@TempDir Path tmp) {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        DefaultWorkspaceFileService svc = new DefaultWorkspaceFileService(mgr, p);

        assertThatThrownBy(() -> svc.readFile("u1", "../outside.txt")).isInstanceOf(SecurityException.class);
    }

    @Test
    void rejectsWriteWhenQuotaExceeded(@TempDir Path tmp) throws Exception {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        p.getWorkspace().setDefaultQuotaMb(1L);
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        DefaultWorkspaceFileService svc = new DefaultWorkspaceFileService(mgr, p);
        char[] block600k = new char[600 * 1024];
        java.util.Arrays.fill(block600k, 'a');
        svc.writeFile("u1", "a.txt", new String(block600k));
        char[] block500k = new char[500 * 1024];
        java.util.Arrays.fill(block500k, 'b');
        assertThatThrownBy(() -> svc.writeFile("u1", "b.txt", new String(block500k)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("配额");
    }
}
