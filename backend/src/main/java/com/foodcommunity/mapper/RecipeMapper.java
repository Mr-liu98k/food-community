package com.foodcommunity.mapper;

import com.foodcommunity.dto.RecipeQuery;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.entity.RecipeIngredient;
import com.foodcommunity.entity.RecipeStep;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜谱主/从表 Mapper。分页查询由 PageHelper 在 Service 层包裹。
 */
public interface RecipeMapper {

    long countByCategory(@Param("categoryId") Long categoryId);

    /** 该作者已通过审核的菜谱数（作者主页用） */
    long countApprovedByUser(@Param("userId") Long userId);

    /** 该作者全部菜谱数（删除用户前引用检查） */
    long countByUser(@Param("userId") Long userId);

    /** 冗余点赞数增减：delta=1 点赞 / -1 取消 */
    int updateLikes(@Param("id") Long id, @Param("delta") int delta);

    /** 通用分页列表（作者/分类/关键词/状态 过滤，联表带作者与分类名） */
    List<Recipe> selectByQuery(@Param("q") RecipeQuery query);

    /** 热门菜谱：已通过，浏览量倒序 */
    List<Recipe> selectHot(@Param("limit") int limit);

    /** 单条详情（联表带作者与分类名） */
    Recipe selectDetail(@Param("id") Long id);

    int insert(Recipe recipe);

    /** 更新内容（不含 views/likes/status/reject_reason） */
    int updateContent(Recipe recipe);

    int updateViews(@Param("id") Long id);

    int updateAudit(@Param("id") Long id, @Param("status") int status, @Param("reason") String reason);

    int deleteById(@Param("id") Long id);

    List<RecipeIngredient> selectIngredients(@Param("recipeId") Long recipeId);

    List<RecipeStep> selectSteps(@Param("recipeId") Long recipeId);

    int insertIngredients(@Param("list") List<RecipeIngredient> ingredients);

    int insertSteps(@Param("list") List<RecipeStep> steps);

    int deleteIngredients(@Param("recipeId") Long recipeId);

    int deleteSteps(@Param("recipeId") Long recipeId);

    // 级联清理互动数据（M2 起有数据）
    int deleteLikes(@Param("recipeId") Long recipeId);

    int deleteFavorites(@Param("recipeId") Long recipeId);

    int deleteComments(@Param("recipeId") Long recipeId);
}
