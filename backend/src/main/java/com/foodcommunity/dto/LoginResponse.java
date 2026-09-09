package com.foodcommunity.dto;

import com.foodcommunity.entity.User;

/**
 * 登录成功响应：token + 用户信息（password 已被实体注解屏蔽，不会外泄）。
 */
public record LoginResponse(String token, User user) {
}
