package cn.lysoy.jingu3.rag.service;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MemoryAugmentationServiceTest {

    @Test
    void injectionDisabled_returnsRaw() {
        Jingu3Properties props = new Jingu3Properties();
        props.getMemory().setInjectionEnabled(false);
        @SuppressWarnings("unchecked")
        ObjectProvider<MilvusMemoryRetrievalService> provider = Mockito.mock(ObjectProvider.class);
        MemoryAugmentationService sut = new MemoryAugmentationService(props, provider);
        assertThat(sut.augmentUserMessageIfEnabled("hi", "001")).isEqualTo("hi");
    }

    @Test
    void retrievalThrows_outerDegradesToRaw() {
        Jingu3Properties props = new Jingu3Properties();
        props.getMemory().setInjectionEnabled(true);
        @SuppressWarnings("unchecked")
        ObjectProvider<MilvusMemoryRetrievalService> provider = Mockito.mock(ObjectProvider.class);
        MilvusMemoryRetrievalService svc = Mockito.mock(MilvusMemoryRetrievalService.class);
        when(provider.getIfAvailable()).thenReturn(svc);
        when(svc.augmentUserMessage(anyString(), anyString())).thenThrow(new RuntimeException("milvus down"));
        MemoryAugmentationService sut = new MemoryAugmentationService(props, provider);
        assertThat(sut.augmentUserMessageIfEnabled("hello", "001")).isEqualTo("hello");
    }
}
