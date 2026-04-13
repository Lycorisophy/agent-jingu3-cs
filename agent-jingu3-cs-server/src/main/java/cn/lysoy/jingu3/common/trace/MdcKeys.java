package cn.lysoy.jingu3.common.trace;

/**
 * SLF4J / Log4j2 {@link org.slf4j.MDC} 键名，与 {@code log4j2-spring.xml} 中 {@code %X{...}} 一致。
 */
public final class MdcKeys {

    public static final String REQUEST_ID = "requestId";

    public static final String TRACE_ID = "traceId";

    private MdcKeys() {
    }
}
