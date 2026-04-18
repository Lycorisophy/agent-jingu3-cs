package cn.lysoy.jingu3.controller.api;

import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.common.trace.SnowflakeIdGenerator;
import cn.lysoy.jingu3.common.vo.SkillListItemVo;
import cn.lysoy.jingu3.common.vo.SkillSubscriptionItemVo;
import cn.lysoy.jingu3.component.ApiExceptionHandler;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.skill.service.SkillService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SkillController.class)
@Import(ApiExceptionHandler.class)
@TestPropertySource(
        properties = {
            "jingu3.skill.api-enabled=true",
        })
class SkillControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SkillService skillService;

    @MockBean
    private UserConstants userConstants;

    @MockBean
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @BeforeEach
    void stubUser() {
        when(userConstants.getId()).thenReturn("001");
        when(snowflakeIdGenerator.nextIdString()).thenReturn("1");
    }

    @Test
    void listReturnsApiResult() throws Exception {
        SkillListItemVo item = new SkillListItemVo();
        item.setId("s1");
        item.setSlug("demo");
        item.setName("Demo");
        when(skillService.listPublicCatalog()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/skills").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data[0].slug").value("demo"));
    }

    @Test
    void subscriptionsDelegatesToService() throws Exception {
        when(skillService.listMySubscriptions(eq("001"))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/skills/subscriptions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(skillService).listMySubscriptions("001");
    }

    @Test
    void getBySlugReturns404WhenNotFound() throws Exception {
        when(skillService.getPublicBySlug("missing"))
                .thenThrow(new ServiceException(ErrorCode.NOT_FOUND, "技能不存在或未公开"));

        mockMvc.perform(get("/api/v1/skills/missing").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.NOT_FOUND.getCode()));
    }

    @Test
    void subscriptionsReturnsSubscriptionVo() throws Exception {
        SkillSubscriptionItemVo row = new SkillSubscriptionItemVo();
        row.setSubscriptionId("us1");
        row.setSkillId("sk1");
        row.setSlug("my-skill");
        when(skillService.listMySubscriptions("001")).thenReturn(List.of(row));

        mockMvc.perform(get("/api/v1/skills/subscriptions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("my-skill"));
    }

    @Test
    void postSubscribeCallsService() throws Exception {
        mockMvc.perform(
                        post("/api/v1/skills/subscriptions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"skillId\":\"sk-abc\"}")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(skillService).subscribe("001", "sk-abc");
    }

    @Test
    void deleteUnsubscribeCallsService() throws Exception {
        mockMvc.perform(delete("/api/v1/skills/subscriptions/sk99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(skillService).unsubscribe("001", "sk99");
    }
}
