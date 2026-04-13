package cn.lysoy.jingu3.memory;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.vo.MemoryEntryVo;
import cn.lysoy.jingu3.config.Jingu3Properties;
import cn.lysoy.jingu3.memory.entity.MemoryEntryEntity;
import cn.lysoy.jingu3.memory.mapper.FactMetadataMapper;
import cn.lysoy.jingu3.memory.mapper.MemoryEntryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class DefaultMemoryServiceTest {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private MemoryEntryMapper memoryEntryMapper;

    @Autowired
    private FactMetadataMapper factMetadataMapper;

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

        assertThat(factMetadataMapper.selectById(vo.getId())).isNotNull();

        assertThat(memoryService.listByUserId("001")).hasSize(1);
        assertThat(memoryService.listByUserId("001").get(0).getFactTag()).isEqualTo("prefs");
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
        assertThat(memoryEntryMapper.selectCount(new QueryWrapper<MemoryEntryEntity>())).isEqualTo(5);
    }
}
