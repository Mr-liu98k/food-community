package com.foodcommunity.common;

/**
 * 当前登录用户的身份快照（拦截器填充进 UserContext）。
 *
 * @param id   用户 ID
 * @param role 角色：0=普通用户 1=管理员
 */
public record LoginUser(Long id, Integer role) {
}
