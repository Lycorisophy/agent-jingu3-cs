package cn.lysoy.jingu3.memory;

import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.vo.MemoryEntryVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.memory.entity.FactMetadataEntity;
import cn.lysoy.jingu3.memory.repo.FactMetadataRepository;
import cn.lysoy.jingu3.memory.repo.MemoryEntryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true"
        })
@Import(DefaultMemoryService.class)
@EnableConfigurationProperties(Jingu3Properties.class)
class DefaultMemoryServiceTest {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private MemoryEntryRepository memoryEntryRepository;

    @Autowired
    private FactMetadataRepository factMetadataRepository;

    @Autowired
    private Jingu3Properties jingu3Properties;

    @Test
    void createFactWithTagAndList() {
        CreateMemoryEntryRequest req = new CreateMemoryEntryRequest();
        req.setUserId("001");
        req.setKind("FACT");
        req.setSummary("s1");
        req.setBody("b1");
        req.setFactTag("prefs");

        MemoryEntryVo vo = memoryService.create(req);
        assertThat(vo.getId()).isNotNull();
        assertThat(vo.getKind()).isEqualTo("FACT");
        assertThat(vo.getFactTag()).isEqualTo("prefs");

        assertThat(factMetadataRepository.findById(vo.getId())).isPresent();

        List<MemoryEntryVo> list = memoryService.listByUserId("001");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getFactTag()).isEqualTo("prefs");
    }

    @Test
    void listRespectsMaxSize() {
        ReflectionTestUtils.setField(jingu3Properties.getMemory(), "maxListSize", 2);
        for (int i = 0; i < 5; i++) {
            CreateMemoryEntryRequest req = new CreateMemoryEntryRequest();
            req.setUserId("u2");
            req.setKind("EVENT");
            req.setSummary("x" + i);
            memoryService.create(req);
        }
        assertThat(memoryService.listByUserId("u2")).hasSize(2);
        assertThat(memoryEntryRepository.count()).isEqualTo(5);
    }
}
