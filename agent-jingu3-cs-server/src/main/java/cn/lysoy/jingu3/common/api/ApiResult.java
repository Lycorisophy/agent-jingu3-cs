package cn.lysoy.jingu3.common.api;

import cn.lysoy.jingu3.common.enums.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一 HTTP 响应体。
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResult<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final long timestamp;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(
                true,
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getDefaultMessage(),
                data,
                System.currentTimeMillis()
        );
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getDefaultMessage());
    }

    public static <T> ApiResult<T> fail(ErrorCode errorCode, String message) {
        return new ApiResult<>(
                false,
                errorCode.getCode(),
                message,
                null,
                System.currentTimeMillis()
        );
    }
}
