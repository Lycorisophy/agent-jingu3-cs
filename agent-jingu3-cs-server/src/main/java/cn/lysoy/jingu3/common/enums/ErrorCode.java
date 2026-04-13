package cn.lysoy.jingu3.common.enums;

import org.springframework.http.HttpStatus;

/**
 * 标准业务错误码（AG = agent 域）；成功码统一为 0。
 */
public enum ErrorCode {

    SUCCESS("0", "成功", HttpStatus.OK),

    BAD_REQUEST("AG_40001", "请求参数错误", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("AG_40002", "参数校验失败", HttpStatus.BAD_REQUEST),

    NOT_FOUND("AG_40401", "资源不存在", HttpStatus.NOT_FOUND),

    TOO_MANY_REQUESTS("AG_42901", "请求过于频繁，请稍后再试", HttpStatus.TOO_MANY_REQUESTS),

    INTERNAL_ERROR("AG_50001", "服务内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
