package com.foodcommunity.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜谱分类。
 */
@Data
public class Category {

    private Long id;

    private String name;

    /** 排序值（小在前） */
    private Integer sort;

    private LocalDateTime createTime;
}
