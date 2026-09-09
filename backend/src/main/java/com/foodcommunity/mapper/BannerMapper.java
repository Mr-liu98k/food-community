package com.foodcommunity.mapper;

import com.foodcommunity.entity.Banner;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 轮播图 Mapper。
 */
public interface BannerMapper {

    /** 管理端分页（联菜谱标题） */
    List<Banner> selectAdminPage(@Param("keyword") String keyword);

    /** 前台列表：仅关联【审核通过】的菜谱（菜谱被删/被拒后自动过滤） */
    List<Banner> selectFrontList();

    Banner selectById(@Param("id") Long id);

    long countByRecipe(@Param("recipeId") Long recipeId);

    int insert(Banner banner);

    int update(Banner banner);

    int deleteById(@Param("id") Long id);
}
