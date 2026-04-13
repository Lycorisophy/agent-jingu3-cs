package cn.lysoy.jingu3.workspace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultWorkspaceFileServiceTest {

    @Test
    void readWriteAndList(@TempDir Path tmp) throws Exception {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        p.getWorkspace().setMaxFileSizeMb(1);
        DefaultWorkspaceFileService svc = new DefaultWorkspaceFileService(p);

        svc.writeFile("u1", "a/b.txt", "hello");
        assertThat(svc.readFile("u1", "a/b.txt")).isEqualTo("hello");
        assertThat(svc.listDirectory("u1", "a")).containsExactly("b.txt");
        assertThat(svc.listDirectory("u1", ".")).contains("a");
    }

    @Test
    void rejectsTraversal(@TempDir Path tmp) {
        Jingu3Properties p = new Jingu3Properties();
        p.getWorkspace().setRootDir(tmp.toString());
        DefaultWorkspaceFileService svc = new DefaultWorkspaceFileService(p);

        assertThatThrownBy(() -> svc.readFile("u1", "../outside.txt")).isInstanceOf(SecurityException.class);
    }
}
