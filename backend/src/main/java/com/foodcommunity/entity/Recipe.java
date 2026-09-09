package com.foodcommunity.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜谱笔记主表。
 * categoryName / authorNickname / authorAvatar 为联表展示字段，
 * 由列表/详情查询的别名列填充（非 user 表物理字段）。
 */
@Data
public class Recipe {

    private Long id;

    private Long userId;

    private Long categoryId;

    private String title;

    /** 封面图 URL（可为空串，前端显示占位图） */
    private String cover;

    /** 简介/做法说明（支持 Markdown） */
    private String description;

    private Integer views;

    private Integer likesCount;

    /** 审核状态：0=待审核 1=已通过 2=已拒绝 */
    private Integer status;

    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    // ---- 联表展示字段（查询别名填充）----
    private String categoryName;

    private String authorNickname;

    private String authorAvatar;
}
