package com.logistics.oms.security;

/**
 * 当前登录用户：userId / 用户名 / 角色 / 关联客户ID（CUSTOMER 角色）。
 * 不可变对象，由 JWT 解析后放入 ThreadLocal。
 */
public class LoginUser {

    private final Long userId;
    private final String username;
    private final String role;
    private final Long customerId;

    public LoginUser(Long userId, String username, String role, Long customerId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.customerId = customerId;
    }

    public Long userId() {
        return userId;
    }

    public String username() {
        return username;
    }

    public String role() {
        return role;
    }

    public Long customerId() {
        return customerId;
    }
}
