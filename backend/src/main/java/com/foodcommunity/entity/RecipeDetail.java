package com.foodcommunity.entity;

import lombok.Data;

import java.util.List;

/**
 * 菜谱详情组装：主表（含作者/分类联表信息）+ 食材清单 + 制作步骤。
 */
@Data
public class RecipeDetail {

    private Recipe recipe;

    private List<RecipeIngredient> ingredients;

    private List<RecipeStep> steps;

    /** 当前登录用户是否已点赞（未登录为 false） */
    private boolean liked;

    /** 当前登录用户是否已收藏（未登录为 false） */
    private boolean favorited;
}
