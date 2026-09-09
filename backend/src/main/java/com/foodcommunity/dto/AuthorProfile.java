package com.foodcommunity.dto;

import com.foodcommunity.entity.User;

/**
 * 作者/达人主页信息（公开）。
 */
public record AuthorProfile(User user, long recipeCount, long storeVisitCount) {
}
