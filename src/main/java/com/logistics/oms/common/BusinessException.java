package com.logistics.oms.common;

import lombok.Getter;

/**
 * 业务异常：code 与 ResultCode 对齐，由 GlobalExceptionHandler 统一转 HTTP 状态。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException unauthorized(String msg) {
        return new BusinessException(ResultCode.UNAUTHORIZED, msg);
    }

    public static BusinessException forbidden(String msg) {
        return new BusinessException(ResultCode.FORBIDDEN, msg);
    }

    public static BusinessException notFound(String msg) {
        return new BusinessException(ResultCode.NOT_FOUND, msg);
    }

    public static BusinessException conflict(String msg) {
        return new BusinessException(ResultCode.CONFLICT, msg);
    }

    public static BusinessException param(String msg) {
        return new BusinessException(ResultCode.PARAM_ERROR, msg);
    }
}
