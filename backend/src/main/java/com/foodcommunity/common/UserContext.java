package com.foodcommunity.common;

/**
 * 登录用户上下文：拦截器解析 JWT 后写入，请求结束自动清理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 当前登录用户 ID，未登录返回 null。 */
    public static Long id() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.id();
    }

    /** 当前登录用户角色，未登录返回 null。 */
    public static Integer role() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.role();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
