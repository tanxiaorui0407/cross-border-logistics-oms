package com.logistics.oms.common;

/**
 * 统一响应码。
 * 0 表示成功；>=400 与 HTTP 状态码对齐，便于网关/前端直接判断。
 */
public final class ResultCode {

    public static final int SUCCESS = 0;
    public static final int PARAM_ERROR = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int ERROR = 500;

    private ResultCode() {
    }
}
