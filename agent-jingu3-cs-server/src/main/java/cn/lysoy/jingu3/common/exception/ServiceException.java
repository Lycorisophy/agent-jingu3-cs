package cn.lysoy.jingu3.common.exception;

import cn.lysoy.jingu3.common.enums.ErrorCode;

/**
 * 业务异常：携带标准 {@link ErrorCode}，由全局处理器转为 {@link cn.lysoy.jingu3.common.api.ApiResult}。
 */
public class ServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detailMessage;

    public ServiceException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.detailMessage = errorCode.getDefaultMessage();
    }

    public ServiceException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public ServiceException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage, cause);
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return detailMessage;
    }
}
