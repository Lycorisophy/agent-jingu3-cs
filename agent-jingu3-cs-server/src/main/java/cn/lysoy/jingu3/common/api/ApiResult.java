package cn.lysoy.jingu3.common.api;

import cn.lysoy.jingu3.common.enums.ErrorCode;

/**
 * 统一 HTTP 响应体。
 *
 * @param success 是否成功
 * @param code    业务码（成功为 0）
 * @param message 说明文案
 * @param data    成功时负载
 * @param timestamp 毫秒时间戳
 */
public record ApiResult<T>(
        boolean success,
        String code,
        String message,
        T data,
        long timestamp
) {

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
