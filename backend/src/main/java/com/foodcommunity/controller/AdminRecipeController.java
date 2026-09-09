package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.dto.AuditRequest;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.entity.RecipeDetail;
import com.foodcommunity.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端菜谱管理（仅管理员）：分页（全状态）、完整预览、审核、删除。
 */
@RestController
@RequestMapping("/api/admin/recipe")
@RequiredArgsConstructor
public class AdminRecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public Result<PageResult<Recipe>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(recipeService.adminPage(page, size, keyword, categoryId, status));
    }

    /** 完整预览（含食材与步骤，不增加浏览量） */
    @GetMapping("/{id}")
    public Result<RecipeDetail> preview(@PathVariable Long id) {
        return Result.ok(recipeService.adminPreview(id));
    }

    /** 审核：status=1 通过 / 2 拒绝（拒绝必填 reason） */
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest req) {
        recipeService.audit(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recipeService.adminDelete(id);
        return Result.ok();
    }
}
