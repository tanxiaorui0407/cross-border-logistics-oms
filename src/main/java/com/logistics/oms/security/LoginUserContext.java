package com.logistics.oms.security;

/**
 * 登录态上下文（ThreadLocal 持有当前请求用户）。
 * 由 JwtInterceptor 在 preHandle 写入、afterCompletion 清理。
 */
public class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 未登录时抛出 401。 */
    public static LoginUser requireLogin() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new com.logistics.oms.common.BusinessException(
                    com.logistics.oms.common.ResultCode.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
