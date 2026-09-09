package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.entity.StoreVisit;
import com.foodcommunity.service.StoreVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台探店浏览（公开）：列表 / 热门 / 详情（弹窗内展示，浏览量自增）。
 */
@RestController
@RequestMapping("/api/store-visit")
@RequiredArgsConstructor
public class PublicStoreVisitController {

    private final StoreVisitService storeVisitService;

    @GetMapping
    public Result<PageResult<StoreVisit>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String keyword) {
        return Result.ok(storeVisitService.publicPage(page, size, keyword));
    }

    @GetMapping("/hot")
    public Result<List<StoreVisit>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(storeVisitService.hot(limit));
    }

    /** 弹窗详情：views+1 */
    @GetMapping("/{id}")
    public Result<StoreVisit> detail(@PathVariable Long id) {
        return Result.ok(storeVisitService.detail(id));
    }
}
