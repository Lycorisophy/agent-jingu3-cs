package cn.lysoy.jingu3.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SSE 与 WebSocket 共用的单条载荷：Jackson 序列化为 JSON 后写入 {@code text/event-stream} 的 data，
 * 或作为 WebSocket 文本帧。字段按 {@link StreamEventType} 选填，未使用字段为 {@code null}（见类上 {@code @JsonInclude}）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StreamEvent {

    private StreamEventType type;
    /** TOKEN */
    private String delta;
    /** STEP_BEGIN / STEP_END */
    private Integer step;
    /** STEP_BEGIN 时的人类可读标签 */
    private String label;
    /** META */
    private String actionMode;
    private String routingSource;
    private String userId;
    private String username;
    /** BLOCK */
    private String block;
    /** ERROR */
    private String error;

    public static StreamEvent meta(
            String actionMode,
            String routingSource,
            String userId,
            String username) {
        return StreamEvent.builder()
                .type(StreamEventType.META)
                .actionMode(actionMode)
                .routingSource(routingSource)
                .userId(userId)
                .username(username)
                .build();
    }

    public static StreamEvent token(String delta) {
        return StreamEvent.builder().type(StreamEventType.TOKEN).delta(delta).build();
    }

    public static StreamEvent stepBegin(int step, String label) {
        return StreamEvent.builder()
                .type(StreamEventType.STEP_BEGIN)
                .step(step)
                .label(label)
                .build();
    }

    public static StreamEvent stepEnd(int step) {
        return StreamEvent.builder().type(StreamEventType.STEP_END).step(step).build();
    }

    public static StreamEvent block(String text) {
        return StreamEvent.builder().type(StreamEventType.BLOCK).block(text).build();
    }

    public static StreamEvent done() {
        return StreamEvent.builder().type(StreamEventType.DONE).build();
    }

    public static StreamEvent error(String message) {
        return StreamEvent.builder().type(StreamEventType.ERROR).error(message).build();
    }
}
