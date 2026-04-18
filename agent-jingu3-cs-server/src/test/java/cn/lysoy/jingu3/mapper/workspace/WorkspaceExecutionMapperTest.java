package cn.lysoy.jingu3.mapper.workspace;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.skill.workspace.constant.WorkspaceExecutionModes;
import cn.lysoy.jingu3.skill.workspace.entity.WorkspaceEntity;
import cn.lysoy.jingu3.skill.workspace.entity.WorkspaceExecutionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class WorkspaceExecutionMapperTest {

    @Autowired
    private WorkspaceMapper workspaceMapper;

    @Autowired
    private WorkspaceExecutionMapper workspaceExecutionMapper;

    @Test
    void selectRecentByUserId() {
        String wid = UUID.randomUUID().toString();
        WorkspaceEntity ws = new WorkspaceEntity();
        ws.setId(wid);
        ws.setUserId("001");
        ws.setRootPath("/tmp/ws");
        ws.setName("001");
        ws.setQuotaMb(1024L);
        ws.setStatus("ACTIVE");
        LocalDateTime now = LocalDateTime.now();
        ws.setCreatedAt(now);
        ws.setUpdatedAt(now);
        workspaceMapper.insert(ws);

        WorkspaceExecutionEntity ex = new WorkspaceExecutionEntity();
        ex.setId(UUID.randomUUID().toString());
        ex.setWorkspaceId(wid);
        ex.setUserId("001");
        ex.setLanguage("python");
        ex.setRunMode(WorkspaceExecutionModes.INLINE);
        ex.setRelativePath(null);
        ex.setCodeHash("abc");
        ex.setStdoutSnippet("out");
        ex.setStderrSnippet("");
        ex.setExitCode(0);
        ex.setDurationMs(12L);
        ex.setSuccess(true);
        ex.setErrorType(null);
        ex.setTimedOut(false);
        ex.setCreatedAt(now);
        workspaceExecutionMapper.insert(ex);

        List<WorkspaceExecutionEntity> list = workspaceExecutionMapper.selectRecentByUserId("001", 10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStdoutSnippet()).isEqualTo("out");
        assertThat(list.get(0).getRunMode()).isEqualTo(WorkspaceExecutionModes.INLINE);
    }
}
