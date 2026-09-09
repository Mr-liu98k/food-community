package com.foodcommunity.service;

import com.foodcommunity.dto.DashboardData;
import com.foodcommunity.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端看板：四项/用户统计 + 近 7 天发布折线（缺失日期补零）+ 分类饼图。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardData summary() {
        DashboardData data = new DashboardData();
        data.setRecipeCount(dashboardMapper.countRecipes());
        data.setCommentCount(dashboardMapper.countComments());
        data.setStoreVisitCount(dashboardMapper.countStoreVisits());
        data.setReservationCount(dashboardMapper.countReservations());
        data.setUserCount(dashboardMapper.countUsers());

        // 折线：以今天为基准往前推 6 天，共 7 天；DB 没有的日期补 0
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        String start = today.minusDays(6).format(fmt);

        Map<String, Long> dayMap = new HashMap<>();
        for (Map<String, Object> row : dashboardMapper.dailyRecipesSince(start)) {
            Object day = row.get("day");
            Object cnt = row.get("cnt");
            if (day != null && cnt != null) {
                dayMap.put(day.toString(), Long.valueOf(cnt.toString()));
            }
        }
        for (int i = 6; i >= 0; i--) {
            String day = today.minusDays(i).format(fmt);
            data.getWeekly().add(new DashboardData.DayCount(day.substring(5), dayMap.getOrDefault(day, 0L)));
        }

        // 饼图
        for (Map<String, Object> row : dashboardMapper.categoryDist()) {
            String name = row.get("name") == null ? "未分类" : row.get("name").toString();
            long cnt = row.get("cnt") == null ? 0 : Long.parseLong(row.get("cnt").toString());
            data.getCategories().add(new DashboardData.CategoryCount(name, cnt));
        }
        return data;
    }
}
