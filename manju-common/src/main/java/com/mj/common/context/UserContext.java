package com.mj.common.context;

import org.springframework.stereotype.Component;

/**
 * 用户上下文 - 基于 ThreadLocal 存储当前请求的用户信息
 * <p>
 * 用于在同一个请求线程内传递用户身份信息（userId、username），
 * 避免在 Controller、Service 层之间显式传递用户参数。
 * </p>
 *
 * <b>注意：</b>请求结束后必须调用 {@link #clear()} 清理 ThreadLocal，
 * 防止线程复用时造成数据污染或内存泄漏。
 */
@Component
public class UserContext {

    private static final ThreadLocal<UserInfo> USER_HOLDER = new ThreadLocal<>();

    /**
     * 存储当前用户信息到 ThreadLocal
     *
     * @param userId   用户ID
     * @param username 用户名
     */
    public static void setUser(Long userId, String username) {
        USER_HOLDER.set(new UserInfo(userId, username));
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，未设置时返回 null
     */
    public static Long getUserId() {
        UserInfo userInfo = USER_HOLDER.get();
        return userInfo != null ? userInfo.userId() : null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，未设置时返回 null
     */
    public static String getUsername() {
        UserInfo userInfo = USER_HOLDER.get();
        return userInfo != null ? userInfo.username() : null;
    }

    /**
     * 清理 ThreadLocal，防止内存泄漏
     * <p>
     * 应在请求完成后（如拦截器的 afterCompletion）中调用
     * </p>
     */
    public static void clear() {
        USER_HOLDER.remove();
    }

    /**
     * 内部用户信息实体
     */
    private record UserInfo(Long userId, String username) {
    }
}
