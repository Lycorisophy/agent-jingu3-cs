package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.trace.HttpTraceHeaders;
import cn.lysoy.jingu3.common.trace.MdcKeys;
import cn.lysoy.jingu3.common.trace.SnowflakeIdGenerator;
import cn.lysoy.jingu3.config.Jingu3Properties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceRequestIdFilterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void setsMdcAndResponseHeaders() throws ServletException, IOException {
        Jingu3Properties props = new Jingu3Properties();
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(props);
        TraceRequestIdFilter filter = new TraceRequestIdFilter(gen);
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> rid = new AtomicReference<>();
        FilterChain chain =
                (HttpServletRequest request, HttpServletResponse response) -> {
                    rid.set(MDC.get(MdcKeys.REQUEST_ID));
                    assertThat(MDC.get(MdcKeys.TRACE_ID)).isNotNull();
                };
        filter.doFilterInternal(req, res, chain);
        assertThat(rid.get()).isNotNull();
        assertThat(res.getHeader(HttpTraceHeaders.REQUEST_ID)).isEqualTo(rid.get());
        assertThat(res.getHeader(HttpTraceHeaders.TRACE_ID)).isNotNull();
    }

    @Test
    void honorsIncomingHeaders() throws ServletException, IOException {
        Jingu3Properties props = new Jingu3Properties();
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(props);
        TraceRequestIdFilter filter = new TraceRequestIdFilter(gen);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(HttpTraceHeaders.REQUEST_ID, "client-req");
        req.addHeader(HttpTraceHeaders.TRACE_ID, "client-trace");
        MockHttpServletResponse res = new MockHttpServletResponse();
        AtomicReference<String> rid = new AtomicReference<>();
        AtomicReference<String> trace = new AtomicReference<>();
        FilterChain chain =
                (HttpServletRequest request, HttpServletResponse response) -> {
                    rid.set(MDC.get(MdcKeys.REQUEST_ID));
                    trace.set(MDC.get(MdcKeys.TRACE_ID));
                };
        filter.doFilterInternal(req, res, chain);
        assertThat(rid.get()).isEqualTo("client-req");
        assertThat(trace.get()).isEqualTo("client-trace");
    }
}
