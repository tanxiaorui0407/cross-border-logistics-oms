package com.logistics.oms.security;

import com.logistics.oms.common.BusinessException;
import com.logistics.oms.common.ResultCode;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * JWT 拦截器：
 * 1. 从 Authorization: Bearer xxx 解析登录用户并放入 ThreadLocal；
 * 2. 校验方法/类上的 @RequireRole：未登录 401，角色不符 403。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            LoginUser user = jwtUtil.parseToken(auth.substring(7));
            LoginUserContext.set(user);
        }

        HandlerMethod hm = (HandlerMethod) handler;
        RequireRole ann = hm.getMethodAnnotation(RequireRole.class);
        if (ann == null) {
            ann = hm.getBeanType().getAnnotation(RequireRole.class);
        }
        if (ann != null) {
            LoginUser user = LoginUserContext.get();
            if (user == null) {
                throw BusinessException.unauthorized("请先登录");
            }
            String[] required = ann.value();
            if (required.length > 0
                    && Arrays.stream(required).noneMatch(r -> r.equals(user.role()) || "ADMIN".equals(user.role()))) {
                throw new BusinessException(ResultCode.FORBIDDEN, "当前角色无权限执行该操作");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        LoginUserContext.clear();
    }
}
