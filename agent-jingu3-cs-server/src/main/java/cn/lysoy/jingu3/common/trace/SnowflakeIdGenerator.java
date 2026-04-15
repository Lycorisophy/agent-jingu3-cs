package cn.lysoy.jingu3.common.trace;

import cn.lysoy.jingu3.config.Jingu3Properties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Twitter Snowflake 风格 64 位 ID（单机内线程安全），用于请求/追踪 ID。
 * <p>时间位、机器位见 {@link Jingu3Properties.Snowflake}。</p>
 */
@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1288834974657L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(Jingu3Properties properties) {
        this.workerId = properties.getSnowflake().getWorkerId();
        this.datacenterId = properties.getSnowflake().getDatacenterId();
    }

    @PostConstruct
    void afterSpringConstruct() {
        validateSnowflakeIds();
    }

    /**
     * 非 Spring 单测在 {@code new SnowflakeIdGenerator(props)} 之后调用，与 {@link PostConstruct} 行为一致。
     */
    public void initForTest() {
        validateSnowflakeIds();
    }

    private void validateSnowflakeIds() {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("jingu3.snowflake.worker-id out of range 0.." + MAX_WORKER_ID);
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("jingu3.snowflake.datacenter-id out of range 0.." + MAX_DATACENTER_ID);
        }
    }

    public synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards");
        }
        if (ts == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                ts = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = ts;
        return ((ts - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    public String nextIdString() {
        return Long.toUnsignedString(nextId());
    }

    private static long waitNextMillis(long last) {
        long ts = System.currentTimeMillis();
        while (ts <= last) {
            ts = System.currentTimeMillis();
        }
        return ts;
    }
}
