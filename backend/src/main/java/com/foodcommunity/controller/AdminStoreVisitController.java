package com.foodcommunity.controller;

import com.foodcommunity.common.PageResult;
import com.foodcommunity.common.Result;
import com.foodcommunity.dto.AuditRequest;
import com.foodcommunity.entity.StoreVisit;
import com.foodcommunity.service.StoreVisitService;
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
 * 管理端探店管理（仅管理员）：分页（全状态）/ 详情 / 审核 / 删除。
 */
@RestController
@RequestMapping("/api/admin/store-visit")
@RequiredArgsConstructor
public class AdminStoreVisitController {

    private final StoreVisitService storeVisitService;

    @GetMapping
    public Result<PageResult<StoreVisit>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(storeVisitService.adminPage(page, size, keyword, status));
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest req) {
        storeVisitService.audit(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        storeVisitService.adminDelete(id);
        return Result.ok();
    }
}
