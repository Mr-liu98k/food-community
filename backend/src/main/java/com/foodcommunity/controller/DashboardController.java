package com.foodcommunity.controller;

import com.foodcommunity.common.Result;
import com.foodcommunity.dto.DashboardData;
import com.foodcommunity.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端看板（仅管理员）：聚合统计接口。
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardData> summary() {
        return Result.ok(dashboardService.summary());
    }
}
