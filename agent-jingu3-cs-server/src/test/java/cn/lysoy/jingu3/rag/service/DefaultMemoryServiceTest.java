package cn.lysoy.jingu3.rag.service;

import cn.lysoy.jingu3.Jingu3Application;
import cn.lysoy.jingu3.rag.service.MemoryService;
import cn.lysoy.jingu3.common.dto.CreateMemoryEntryRequest;
import cn.lysoy.jingu3.common.dto.UpdateMemoryEntryRequest;
import cn.lysoy.jingu3.common.exception.ServiceException;
import cn.lysoy.jingu3.mapper.memory.MemoryEntryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = Jingu3Application.class,
        properties = {"jingu3.cron.scheduler-enabled=false"})
@Transactional
class DefaultMemoryServiceTest {

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private MemoryEntryMapper memoryEntryMapper;

    @Test
    void updateSummaryAndDelete() {
        CreateMemoryEntryRequest c = new CreateMemoryEntryRequest();
        c.setUserId("001");
        c.setKind("EVENT");
        c.setSummary("old");
        c.setBody("b");
        var created = memoryService.create(c);
        long id = created.getId();

        UpdateMemoryEntryRequest u = new UpdateMemoryEntryRequest();
        u.setUserId("001");
        u.setSummary("new");
        var updated = memoryService.update(id, u);
        assertThat(updated.getSummary()).isEqualTo("new");

        memoryService.delete(id, "001");
        assertThat(memoryEntryMapper.selectById(id)).isNull();
    }

    @Test
    void updateRejectsWrongUser() {
        CreateMemoryEntryRequest c = new CreateMemoryEntryRequest();
        c.setUserId("001");
        c.setKind("EVENT");
        c.setSummary("x");
        var created = memoryService.create(c);

        UpdateMemoryEntryRequest u = new UpdateMemoryEntryRequest();
        u.setUserId("002");
        u.setSummary("y");
        assertThatThrownBy(() -> memoryService.update(created.getId(), u)).isInstanceOf(ServiceException.class);
    }

    @Test
    void updateRequiresAtLeastOneField() {
        CreateMemoryEntryRequest c = new CreateMemoryEntryRequest();
        c.setUserId("001");
        c.setKind("EVENT");
        c.setSummary("x");
        var created = memoryService.create(c);

        UpdateMemoryEntryRequest u = new UpdateMemoryEntryRequest();
        u.setUserId("001");
        assertThatThrownBy(() -> memoryService.update(created.getId(), u)).isInstanceOf(ServiceException.class);
    }

    @Test
    void switchToFactWithTag() {
        CreateMemoryEntryRequest c = new CreateMemoryEntryRequest();
        c.setUserId("001");
        c.setKind("EVENT");
        c.setSummary("e");
        c.setBody("body");
        var created = memoryService.create(c);

        UpdateMemoryEntryRequest u = new UpdateMemoryEntryRequest();
        u.setUserId("001");
        u.setKind("FACT");
        u.setFactTag("tag1");
        var vo = memoryService.update(created.getId(), u);
        assertThat(vo.getKind()).isEqualTo("FACT");
        assertThat(vo.getFactTag()).isEqualTo("tag1");
    }
}
