package com.logistics.oms.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色控制注解。
 * value 为空数组 = 仅要求登录；非空 = 登录且角色匹配（ADMIN 拥有全部角色权限）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    String[] value() default {};
}
