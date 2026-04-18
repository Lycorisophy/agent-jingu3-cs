package cn.lysoy.jingu3.controller.api;

import cn.lysoy.jingu3.common.trace.SnowflakeIdGenerator;
import cn.lysoy.jingu3.common.vo.ToolListItemVo;
import cn.lysoy.jingu3.component.ApiExceptionHandler;
import cn.lysoy.jingu3.skill.tool.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ToolController.class)
@Import(ApiExceptionHandler.class)
@TestPropertySource(
        properties = {
            "jingu3.tool.catalog-api-enabled=true",
        })
class ToolControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToolRegistry toolRegistry;

    @MockBean
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @BeforeEach
    void stubTraceIds() {
        when(snowflakeIdGenerator.nextIdString()).thenReturn("1");
    }

    @Test
    void listReturnsApiResultWithRiskLevel() throws Exception {
        ToolListItemVo row = new ToolListItemVo();
        row.setId("calculator");
        row.setDescription("expr");
        row.setRiskLevel("LOW");
        when(toolRegistry.buildCatalogList()).thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/tools").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].id").value("calculator"))
                .andExpect(jsonPath("$.data[0].riskLevel").value("LOW"));
    }
}
