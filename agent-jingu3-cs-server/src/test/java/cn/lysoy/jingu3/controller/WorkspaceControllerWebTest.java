package cn.lysoy.jingu3.controller;

import cn.lysoy.jingu3.common.vo.WorkspaceExecutionItemVo;
import cn.lysoy.jingu3.common.vo.WorkspaceFileStatsVo;
import cn.lysoy.jingu3.common.vo.WorkspaceSummaryVo;
import cn.lysoy.jingu3.component.ApiExceptionHandler;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.workspace.service.WorkspaceManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkspaceController.class)
@Import(ApiExceptionHandler.class)
@TestPropertySource(
        properties = {
            "jingu3.workspace.enabled=true",
            "jingu3.workspace.rest-api-enabled=true",
        })
class WorkspaceControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkspaceManagementService workspaceManagementService;

    @MockBean
    private UserConstants userConstants;

    @BeforeEach
    void stubUser() {
        when(userConstants.getId()).thenReturn("001");
    }

    @Test
    void summaryReturnsWorkspaceId() throws Exception {
        WorkspaceSummaryVo vo = new WorkspaceSummaryVo();
        vo.setWorkspaceId("wid-1");
        vo.setUserId("001");
        vo.setRootPath("/tmp/ws");
        vo.setQuotaMb(1024L);
        vo.setFileCount(0L);
        vo.setTotalSizeBytes(0L);
        when(workspaceManagementService.getSummary("001")).thenReturn(vo);

        mockMvc.perform(get("/api/v1/workspace").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workspaceId").value("wid-1"));
    }

    @Test
    void statsReturnsCounts() throws Exception {
        WorkspaceFileStatsVo st = new WorkspaceFileStatsVo();
        st.setQuotaMb(512L);
        st.setFileCount(3L);
        st.setTotalSizeBytes(100L);
        when(workspaceManagementService.getFileStats("001")).thenReturn(st);

        mockMvc.perform(get("/api/v1/workspace/stats").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileCount").value(3));
    }

    @Test
    void resetInvokesService() throws Exception {
        mockMvc.perform(post("/api/v1/workspace/reset").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(workspaceManagementService).resetWorkspace("001");
    }

    @Test
    void deleteInvokesService() throws Exception {
        mockMvc.perform(delete("/api/v1/workspace").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(workspaceManagementService).deleteWorkspace("001");
    }

    @Test
    void executionsReturnsList() throws Exception {
        when(workspaceManagementService.listExecutions(eq("001"), eq(5)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/workspace/executions").param("limit", "5").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(workspaceManagementService).listExecutions("001", 5);
    }

    @Test
    void executionsDefaultLimit() throws Exception {
        when(workspaceManagementService.listExecutions(eq("001"), isNull()))
                .thenReturn(List.of(new WorkspaceExecutionItemVo()));

        mockMvc.perform(get("/api/v1/workspace/executions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(workspaceManagementService).listExecutions("001", null);
    }
}
