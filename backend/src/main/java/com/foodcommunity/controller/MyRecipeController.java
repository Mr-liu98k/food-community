package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.dto.RecipeSaveRequest;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.entity.RecipeDetail;
import com.foodcommunity.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的菜谱（登录）：发布/编辑/删除/我的列表。
 * 编辑后自动回到待审核，需管理员重新审核。
 */
@RestController
@RequestMapping("/api/my/recipe")
@RequiredArgsConstructor
public class MyRecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public Result<PageResult<Recipe>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(recipeService.myPage(UserContext.id(), page, size, status));
    }

    /** 我的菜谱详情（任何状态，编辑回填用） */
    @GetMapping("/{id}")
    public Result<RecipeDetail> detail(@PathVariable Long id) {
        return Result.ok(recipeService.myDetail(UserContext.id(), id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody RecipeSaveRequest req) {
        return Result.ok(recipeService.save(UserContext.id(), null, req));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody RecipeSaveRequest req) {
        return Result.ok(recipeService.save(UserContext.id(), id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recipeService.delete(UserContext.id(), id);
        return Result.ok();
    }
}
