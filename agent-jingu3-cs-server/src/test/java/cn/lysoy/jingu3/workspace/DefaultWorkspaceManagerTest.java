package cn.lysoy.jingu3.skill.workspace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultWorkspaceManagerTest {

    @Test
    void getOrCreateAndStats(@TempDir Path tmp) throws Exception {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        p.getWorkspace().setDefaultQuotaMb(512L);
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);

        Workspace w = mgr.getOrCreateWorkspace("u1");
        assertThat(w.getUserId()).isEqualTo("u1");
        assertThat(w.getQuotaMb()).isEqualTo(512L);
        assertThat(w.getRootPath()).isEqualTo(tmp.resolve("u1").normalize().toString());

        Path f = mgr.resolveUserRoot("u1").resolve("x.txt");
        Files.writeString(f, "ab");

        WorkspaceStats st = mgr.getStats("u1");
        assertThat(st.getFileCount()).isEqualTo(1);
        assertThat(st.getTotalSizeBytes()).isEqualTo(2);
        assertThat(st.getQuotaMb()).isEqualTo(512L);
    }

    @Test
    void resetClearsChildren(@TempDir Path tmp) throws Exception {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        Files.writeString(mgr.resolveUserRoot("u1").resolve("a.txt"), "x");
        mgr.resetWorkspace("u1");
        assertThat(Files.list(mgr.resolveUserRoot("u1")).toList()).isEmpty();
    }

    @Test
    void deleteRemovesUserDir(@TempDir Path tmp) throws Exception {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        mgr.resolveUserRoot("u1");
        mgr.deleteWorkspace("u1");
        assertThat(Files.exists(tmp.resolve("u1"))).isFalse();
    }

    @Test
    void getWorkspaceEmptyWhenMissing(@TempDir Path tmp) throws Exception {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        assertThat(mgr.getWorkspace("ghost")).isEmpty();
    }

    @Test
    void rejectsBadUserId(@TempDir Path tmp) {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        DefaultWorkspaceManager mgr = new DefaultWorkspaceManager(p);
        assertThatThrownBy(() -> mgr.resolveUserRoot("../evil")).isInstanceOf(SecurityException.class);
    }
}
