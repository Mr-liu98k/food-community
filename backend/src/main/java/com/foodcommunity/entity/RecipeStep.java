package com.foodcommunity.entity;

import lombok.Data;

/**
 * 制作步骤（recipe 的从表）。
 */
@Data
public class RecipeStep {

    private Long id;

    private Long recipeId;

    /** 步骤文字（支持 Markdown） */
    private String content;

    private Integer sort;
}
