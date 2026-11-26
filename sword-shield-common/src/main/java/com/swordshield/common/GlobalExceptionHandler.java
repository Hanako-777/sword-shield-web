package com.swordshield.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<Object> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("业务校验失败: {}", e.getMessage());
        return Response.error(ErrorCode.BAD_REQUEST.code, e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public Response<Object> handleSecurityException(SecurityException e) {
        log.warn("权限校验失败: {}", e.getMessage());
        return Response.error(ErrorCode.UNAUTHORIZED.code, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Response<Object> handleException(Exception e) {
        log.error("未捕获异常", e);
        return Response.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.SERVER_ERROR.message);
    }
}
