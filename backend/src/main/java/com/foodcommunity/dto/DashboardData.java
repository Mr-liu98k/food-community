package com.foodcommunity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端看板聚合数据。
 */
@Data
public class DashboardData {

    // ---- 统计卡 ----
    private long recipeCount;

    private long commentCount;

    private long storeVisitCount;

    private long reservationCount;

    private long userCount;

    // ---- 折线图：近 7 天（含今天）每日菜谱发布量，服务端补零，旧→新 ----
    private List<DayCount> weekly = new ArrayList<>();

    // ---- 饼图：各分类菜谱数 ----
    private List<CategoryCount> categories = new ArrayList<>();

    @Data
    public static class DayCount {

        private String date;

        private long count;

        public DayCount(String date, long count) {
            this.date = date;
            this.count = count;
        }
    }

    @Data
    public static class CategoryCount {

        private String name;

        private long count;

        public CategoryCount(String name, long count) {
            this.name = name;
            this.count = count;
        }
    }
}
