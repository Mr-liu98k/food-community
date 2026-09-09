package com.foodcommunity.service;

import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.mapper.InteractionMapper;
import com.foodcommunity.mapper.RecipeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 互动模块：点赞 / 收藏（切换式，幂等）。
 * likes_count 为 recipe 冗余计数：插入/删除成功才 ±1，重复操作不会错乱。
 */
@Service
@RequiredArgsConstructor
public class InteractionService {

    private final InteractionMapper interactionMapper;
    private final RecipeMapper recipeMapper;

    public void like(Long userId, Long recipeId) {
        requirePublished(recipeId);
        if (interactionMapper.insertLike(recipeId, userId) > 0) {
            recipeMapper.updateLikes(recipeId, 1);
        }
    }

    public void unlike(Long userId, Long recipeId) {
        if (interactionMapper.deleteLike(recipeId, userId) > 0) {
            recipeMapper.updateLikes(recipeId, -1);
        }
    }

    public void favorite(Long userId, Long recipeId) {
        requirePublished(recipeId);
        interactionMapper.insertFavorite(recipeId, userId);
    }

    public void unfavorite(Long userId, Long recipeId) {
        interactionMapper.deleteFavorite(recipeId, userId);
    }

    /** 我的收藏分页 */
    public PageResult<Recipe> favoritePage(Long userId, int page, int size) {
        PageHelper.startPage(page, size);
        PageInfo<Recipe> pageInfo = new PageInfo<>(interactionMapper.selectMyFavoritePage(userId));
        return PageResult.of(pageInfo);
    }

    public boolean isLiked(Long userId, Long recipeId) {
        return userId != null && interactionMapper.countLike(recipeId, userId) > 0;
    }

    public boolean isFavorited(Long userId, Long recipeId) {
        return userId != null && interactionMapper.countFavorite(recipeId, userId) > 0;
    }

    private void requirePublished(Long recipeId) {
        Recipe recipe = recipeMapper.selectDetail(recipeId);
        if (recipe == null || recipe.getStatus() != 1) {
            throw new BusinessException("菜谱不存在或尚未通过审核");
        }
    }
}
