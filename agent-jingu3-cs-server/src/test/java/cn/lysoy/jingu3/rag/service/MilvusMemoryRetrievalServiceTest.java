package cn.lysoy.jingu3.rag.service;

import cn.lysoy.jingu3.common.constant.PromptFragments;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.mapper.memory.MemoryEntryMapper;
import cn.lysoy.jingu3.rag.MemoryEntryKind;
import cn.lysoy.jingu3.rag.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.rag.integration.embedding.OllamaEmbeddingClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class MilvusMemoryRetrievalServiceTest {

    @Test
    void searchFormattedBlocks_preservesMilvusIdOrder() {
        Jingu3Properties props = new Jingu3Properties();
        props.getMemory().setRetrievalTopK(5);

        OllamaEmbeddingClient embed = Mockito.mock(OllamaEmbeddingClient.class);
        when(embed.embed("query")).thenReturn(new float[] {0.1f, 0.2f});

        MilvusMemoryVectorService milvus = Mockito.mock(MilvusMemoryVectorService.class);
        when(milvus.searchSimilar(eq("001"), any(float[].class), eq(5))).thenReturn(List.of(30L, 10L, 20L));

        MemoryEntryEntity e10 = new MemoryEntryEntity();
        e10.setId(10L);
        e10.setKind(MemoryEntryKind.FACT);
        e10.setSummary("B");
        e10.setBody("b");
        MemoryEntryEntity e20 = new MemoryEntryEntity();
        e20.setId(20L);
        e20.setKind(MemoryEntryKind.FACT);
        e20.setSummary("C");
        e20.setBody("c");
        MemoryEntryEntity e30 = new MemoryEntryEntity();
        e30.setId(30L);
        e30.setKind(MemoryEntryKind.FACT);
        e30.setSummary("A");
        e30.setBody("a");

        MemoryEntryMapper mapper = Mockito.mock(MemoryEntryMapper.class);
        when(mapper.selectByIds(List.of(30L, 10L, 20L))).thenReturn(Arrays.asList(e20, e10, e30));

        MilvusMemoryRetrievalService sut =
                new MilvusMemoryRetrievalService(props, embed, milvus, mapper);

        String block = sut.searchFormattedBlocks("query", "001");

        assertThat(block).startsWith(PromptFragments.MEMORY_REFERENCE_HEADER);
        int iA = block.indexOf("A");
        int iB = block.indexOf("B");
        int iC = block.indexOf("C");
        assertThat(iA).isGreaterThan(0);
        assertThat(iA).isLessThan(iB);
        assertThat(iB).isLessThan(iC);
    }

    @Test
    void searchFormattedBlocks_blankQuery_returnsEmpty() {
        Jingu3Properties props = new Jingu3Properties();
        MilvusMemoryRetrievalService sut = new MilvusMemoryRetrievalService(
                props,
                Mockito.mock(OllamaEmbeddingClient.class),
                Mockito.mock(MilvusMemoryVectorService.class),
                Mockito.mock(MemoryEntryMapper.class));
        assertThat(sut.searchFormattedBlocks("  ", "001")).isEmpty();
        assertThat(sut.searchFormattedBlocks(null, "001")).isEmpty();
    }
}
