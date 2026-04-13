package cn.lysoy.jingu3.config;

import cn.lysoy.jingu3.common.trace.MdcKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void copiesMdcToWorkerThread() {
        MDC.put(MdcKeys.REQUEST_ID, "req-async-test");
        MDC.put(MdcKeys.TRACE_ID, "trace-async-test");
        MdcTaskDecorator decorator = new MdcTaskDecorator();
        AtomicBoolean seen = new AtomicBoolean();
        Runnable inner =
                () -> {
                    assertThat(MDC.get(MdcKeys.REQUEST_ID)).isEqualTo("req-async-test");
                    assertThat(MDC.get(MdcKeys.TRACE_ID)).isEqualTo("trace-async-test");
                    seen.set(true);
                };
        decorator.decorate(inner).run();
        assertThat(seen).isTrue();
    }
}
