package com.foodcommunity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 轮播图新增/编辑请求。
 */
@Data
public class BannerSaveRequest {

    /** 关联菜谱（后端校验：必须存在且已审核通过） */
    @NotNull(message = "请选择要关联的菜谱")
    private Long recipeId;

    private String image;

    private Integer sort;
}
