package cn.lysoy.jingu3.skill.tool;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.component.UserConstants;
import cn.lysoy.jingu3.rag.service.MilvusMemoryRetrievalService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class MemorySearchToolTest {

    @Test
    void execute_delegatesToRetrieval() throws ToolExecutionException {
        MilvusMemoryRetrievalService retrieval = Mockito.mock(MilvusMemoryRetrievalService.class);
        when(retrieval.searchFormattedBlocks(eq("cats"), eq("001"))).thenReturn(PromptFragments.MEMORY_REFERENCE_HEADER + "- x\n");
        UserConstants users = Mockito.mock(UserConstants.class);
        when(users.getId()).thenReturn("001");
        MemorySearchTool tool = new MemorySearchTool(retrieval, users);
        assertThat(tool.execute("cats")).contains("【参考记忆】");
    }

    @Test
    void execute_blankInput_throws() {
        MilvusMemoryRetrievalService retrieval = Mockito.mock(MilvusMemoryRetrievalService.class);
        UserConstants users = Mockito.mock(UserConstants.class);
        when(users.getId()).thenReturn("001");
        MemorySearchTool tool = new MemorySearchTool(retrieval, users);
        assertThatThrownBy(() -> tool.execute(" ")).isInstanceOf(ToolExecutionException.class);
    }
}
