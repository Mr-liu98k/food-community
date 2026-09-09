package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.common.UserContext;
import com.foodcommunity.dto.StoreVisitSaveRequest;
import com.foodcommunity.entity.StoreVisit;
import com.foodcommunity.service.StoreVisitService;
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
 * 我的探店（登录）：发布/编辑/删除/列表/详情。编辑后回到待审核。
 */
@RestController
@RequestMapping("/api/my/store-visit")
@RequiredArgsConstructor
public class MyStoreVisitController {

    private final StoreVisitService storeVisitService;

    @GetMapping
    public Result<PageResult<StoreVisit>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(storeVisitService.myPage(UserContext.id(), page, size, status));
    }

    @GetMapping("/{id}")
    public Result<StoreVisit> detail(@PathVariable Long id) {
        return Result.ok(storeVisitService.myDetail(UserContext.id(), id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody StoreVisitSaveRequest req) {
        return Result.ok(storeVisitService.save(UserContext.id(), null, req));
    }

    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @Valid @RequestBody StoreVisitSaveRequest req) {
        return Result.ok(storeVisitService.save(UserContext.id(), id, req));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        storeVisitService.delete(UserContext.id(), id);
        return Result.ok();
    }
}
