package com.foodcommunity.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页轮播图。
 * recipeTitle 为联表展示字段（关联的菜谱标题）。
 */
@Data
public class Banner {

    private Long id;

    /** 关联的菜谱（必须已审核通过） */
    private Long recipeId;

    /** 轮播图 URL（空串时前端显示渐变占位） */
    private String image;

    private Integer sort;

    private LocalDateTime createTime;

    // ---- 联表字段 ----
    private String recipeTitle;
}
