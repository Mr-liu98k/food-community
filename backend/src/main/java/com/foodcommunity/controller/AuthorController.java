package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.dto.AuthorProfile;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.entity.StoreVisit;
import com.foodcommunity.service.RecipeService;
import com.foodcommunity.service.StoreVisitService;
import com.foodcommunity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 作者/达人主页（公开）：用户信息与统计 + 已通过菜谱/探店分页。
 */
@RestController
@RequestMapping("/api/user/{id}")
@RequiredArgsConstructor
public class AuthorController {

    private final UserService userService;
    private final RecipeService recipeService;
    private final StoreVisitService storeVisitService;

    @GetMapping("/profile")
    public Result<AuthorProfile> profile(@PathVariable Long id) {
        return Result.ok(userService.authorProfile(id));
    }

    @GetMapping("/recipes")
    public Result<PageResult<Recipe>> recipes(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.ok(recipeService.authorPublicPage(id, page, size));
    }

    @GetMapping("/store-visits")
    public Result<PageResult<StoreVisit>> storeVisits(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(storeVisitService.authorPage(id, page, size));
    }
}
