package cn.lysoy.jingu3.service.context.stream;

/**
 * 将异常信息转为适合 SSE/WebSocket 用户可读短句（避免堆栈直出）。
 */
public final class StreamErrorMessages {

    private static final int MAX_LEN = 480;

    private StreamErrorMessages() {}

    public static String fromThrowable(Throwable error) {
        if (error == null) {
            return "对话处理失败，请稍后重试。";
        }
        String raw = error.getMessage();
        if (raw == null || raw.isBlank()) {
            raw = error.getClass().getSimpleName();
        }
        raw = raw.replace('\r', ' ').replace('\n', ' ').trim();
        if (raw.length() > MAX_LEN) {
            raw = raw.substring(0, MAX_LEN) + "…";
        }
        return "对话处理失败：" + raw;
    }

    public static String fromMessage(String message) {
        if (message == null || message.isBlank()) {
            return "对话处理失败，请稍后重试。";
        }
        String raw = message.replace('\r', ' ').replace('\n', ' ').trim();
        if (raw.length() > MAX_LEN) {
            raw = raw.substring(0, MAX_LEN) + "…";
        }
        return "对话处理失败：" + raw;
    }
}
