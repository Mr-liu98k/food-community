package com.foodcommunity.controller;

import com.foodcommunity.common.Result;
import com.foodcommunity.entity.Banner;
import com.foodcommunity.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台轮播（公开）：首页展示，仅关联审核通过的菜谱。
 */
@RestController
@RequestMapping("/api/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/list")
    public Result<List<Banner>> list() {
        return Result.ok(bannerService.frontList());
    }
}
