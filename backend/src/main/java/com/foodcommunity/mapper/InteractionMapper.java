package com.foodcommunity.mapper;

import com.foodcommunity.entity.Recipe;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 互动 Mapper：点赞 / 收藏（都无独立实体，SQL 直写）。
 */
public interface InteractionMapper {

    // ---- 点赞 ----
    int insertLike(@Param("recipeId") Long recipeId, @Param("userId") Long userId);

    int deleteLike(@Param("recipeId") Long recipeId, @Param("userId") Long userId);

    int countLike(@Param("recipeId") Long recipeId, @Param("userId") Long userId);

    // ---- 收藏 ----
    int insertFavorite(@Param("recipeId") Long recipeId, @Param("userId") Long userId);

    int deleteFavorite(@Param("recipeId") Long recipeId, @Param("userId") Long userId);

    int countFavorite(@Param("recipeId") Long recipeId, @Param("userId") Long userId);

    /** 我的收藏分页：JOIN recipe（含分类/作者联表字段） */
    List<Recipe> selectMyFavoritePage(@Param("userId") Long userId);
}
