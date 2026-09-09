package com.foodcommunity.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 管理端数据看板聚合查询。
 */
public interface DashboardMapper {

    long countRecipes();

    long countComments();

    long countStoreVisits();

    long countReservations();

    /** 某日期起每日发布菜谱数（DATE_FORMAT 输出 'yyyy-MM-dd' 字符串） */
    List<Map<String, Object>> dailyRecipesSince(@Param("startDate") String startDate);

    /** 各分类菜谱数（LEFT JOIN 保留空分类，按 sort 排序） */
    List<Map<String, Object>> categoryDist();

    /** 总用户数 */
    long countUsers();
}
