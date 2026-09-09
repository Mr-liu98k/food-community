package com.foodcommunity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布/编辑菜谱请求：主表字段 + 食材清单 + 制作步骤（同事务提交）。
 */
@Data
public class RecipeSaveRequest {

    @NotNull(message = "请选择菜谱分类")
    private Long categoryId;

    @NotBlank(message = "菜谱标题不能为空")
    @Size(max = 100, message = "标题最长 100 字")
    private String title;

    @NotBlank(message = "请上传封面图")
    private String cover;

    @Size(max = 5000, message = "简介内容过长")
    private String description;

    @Valid
    @NotEmpty(message = "请至少添加一种食材")
    private List<IngredientItem> ingredients = new ArrayList<>();

    @Valid
    @NotEmpty(message = "请至少添加一个制作步骤")
    private List<StepItem> steps = new ArrayList<>();

    /** 食材行：name=鸡蛋，amount=2 个 */
    @Data
    public static class IngredientItem {

        @NotBlank(message = "食材名称不能为空")
        @Size(max = 50)
        private String name;

        @NotBlank(message = "食材用量不能为空")
        @Size(max = 50)
        private String amount;
    }

    /** 步骤行：content=步骤文字 */
    @Data
    public static class StepItem {

        @NotBlank(message = "步骤内容不能为空")
        @Size(max = 2000)
        private String content;
    }
}
