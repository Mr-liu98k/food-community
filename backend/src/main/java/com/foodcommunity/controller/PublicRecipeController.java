package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.entity.RecipeDetail;
import com.foodcommunity.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台菜谱浏览（公开）：分页列表（仅已通过）、热门、详情（浏览量自增）。
 */
@RestController
@RequestMapping("/api/recipe")
@RequiredArgsConstructor
public class PublicRecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public Result<PageResult<Recipe>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(recipeService.publicPage(page, size, categoryId, keyword));
    }

    @GetMapping("/hot")
    public Result<List<Recipe>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(recipeService.hot(limit));
    }

    @GetMapping("/{id}")
    public Result<RecipeDetail> detail(@PathVariable Long id) {
        return Result.ok(recipeService.detail(id, UserContext.id()));
    }
}
