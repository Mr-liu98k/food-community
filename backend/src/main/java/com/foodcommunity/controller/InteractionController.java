package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 互动：点赞 / 收藏 / 我的收藏（写操作需登录）。
 */
@RestController
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/api/recipe/{recipeId}/like")
    public Result<Void> like(@PathVariable Long recipeId) {
        interactionService.like(UserContext.id(), recipeId);
        return Result.ok();
    }

    @DeleteMapping("/api/recipe/{recipeId}/like")
    public Result<Void> unlike(@PathVariable Long recipeId) {
        interactionService.unlike(UserContext.id(), recipeId);
        return Result.ok();
    }

    @PostMapping("/api/recipe/{recipeId}/favorite")
    public Result<Void> favorite(@PathVariable Long recipeId) {
        interactionService.favorite(UserContext.id(), recipeId);
        return Result.ok();
    }

    @DeleteMapping("/api/recipe/{recipeId}/favorite")
    public Result<Void> unfavorite(@PathVariable Long recipeId) {
        interactionService.unfavorite(UserContext.id(), recipeId);
        return Result.ok();
    }

    @GetMapping("/api/my/favorites")
    public Result<PageResult<Recipe>> favorites(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return Result.ok(interactionService.favoritePage(UserContext.id(), page, size));
    }
}
