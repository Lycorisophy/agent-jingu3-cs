package cn.lysoy.jingu3.component;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.common.enums.ErrorCode;
import cn.lysoy.jingu3.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常：统一转为 {@link ApiResult}，业务码见 {@link ErrorCode}。
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResult<Void>> serviceException(ServiceException ex) {
        ErrorCode ec = ex.getErrorCode();
        log.warn("ServiceException code={} msg={}", ec.getCode(), ex.getMessage());
        return ResponseEntity
                .status(ec.getHttpStatus())
                .body(ApiResult.fail(ec, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> illegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgument: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(ApiResult.fail(ErrorCode.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> validation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getDefaultMessage());
        log.warn("Validation: {}", msg);
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiResult.fail(ErrorCode.VALIDATION_ERROR, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> fallback(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(ApiResult.fail(ErrorCode.INTERNAL_ERROR));
    }
}
