package com.foodcommunity.entity;

import lombok.Data;

/**
 * 食材清单（recipe 的从表）。
 */
@Data
public class RecipeIngredient {

    private Long id;

    private Long recipeId;

    /** 食材名称，如：鸡蛋 */
    private String name;

    /** 用量，如：2 个 */
    private String amount;

    private Integer sort;
}
