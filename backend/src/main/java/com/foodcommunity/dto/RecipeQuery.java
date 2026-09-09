package com.foodcommunity.dto;

import lombok.Data;

/**
 * 菜谱列表查询参数（公开页 / 我的 / 管理端共用）。
 * userId 非空 → 只看某作者；status 非空 → 按状态过滤。
 */
@Data
public class RecipeQuery {

    /** 标题模糊关键词 */
    private String keyword;

    private Long categoryId;

    /** 审核状态：0=待审核 1=已通过 2=已拒绝（null=不限） */
    private Integer status;

    /** 作者 ID（null=不限） */
    private Long userId;
}
