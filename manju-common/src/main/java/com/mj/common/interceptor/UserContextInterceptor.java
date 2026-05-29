package com.mj.common.interceptor;

import com.mj.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * <p>
 * 从网关注入的请求头（X-User-Id、X-User-Name）中提取用户信息，
 * 存入 {@link UserContext}，并在请求完成后清理。
 * </p>
 * <p>
 * 适用场景：下游 MVC 微服务（user、task 等），网关已完成 JWT 认证并透传用户信息。
 * </p>
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从网关透传的请求头中获取用户信息
        String userIdHeader = request.getHeader("X-User-Id");
        String usernameHeader = request.getHeader("X-User-Name");

        if (userIdHeader != null && usernameHeader != null) {
            UserContext.setUser(Long.valueOf(userIdHeader), usernameHeader);
            log.debug("UserContext 已设置, userId: {}, username: {}", userIdHeader, usernameHeader);
        }

        // 始终放行（即使无用户信息），由具体接口决定是否需要登录态
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}