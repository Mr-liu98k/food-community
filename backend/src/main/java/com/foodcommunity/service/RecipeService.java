package com.foodcommunity.service;

import cn.hutool.core.util.StrUtil;
import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.dto.AuditRequest;
import com.foodcommunity.dto.RecipeQuery;
import com.foodcommunity.dto.RecipeSaveRequest;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.entity.RecipeDetail;
import com.foodcommunity.entity.RecipeIngredient;
import com.foodcommunity.entity.RecipeStep;
import com.foodcommunity.mapper.InteractionMapper;
import com.foodcommunity.mapper.RecipeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜谱模块：我的发布/编辑/删除、公开列表与详情（浏览量自增）、
 * 热门、管理端分页/预览/审核。主从表在发布与编辑中同事务提交。
 */
@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeMapper recipeMapper;
    private final InteractionMapper interactionMapper;

    // ==================== 用户端：我的菜谱 ====================

    public PageResult<Recipe> myPage(Long userId, int page, int size, Integer status) {
        RecipeQuery query = new RecipeQuery();
        query.setUserId(userId);
        query.setStatus(status);
        return doPage(page, size, query);
    }

    /** 我的菜谱详情（任何状态可见，供编辑回填） */
    public RecipeDetail myDetail(Long userId, Long recipeId) {
        Recipe recipe = requireRecipe(recipeId);
        if (!recipe.getUserId().equals(userId)) {
            throw new BusinessException("无权查看他人的菜谱");
        }
        return assembleDetail(recipe);
    }

    /**
     * 发布或编辑菜谱（编辑时整单替换从表并重置为待审核）。
     * recipeId == null → 新增；否则必须属于当前用户。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long userId, Long recipeId, RecipeSaveRequest req) {
        Recipe recipe = new Recipe();
        recipe.setUserId(userId);
        recipe.setCategoryId(req.getCategoryId());
        recipe.setTitle(req.getTitle().trim());
        recipe.setCover(req.getCover());
        recipe.setDescription(req.getDescription());

        if (recipeId == null) {
            recipe.setStatus(0);
            recipeMapper.insert(recipe);
        } else {
            Recipe exist = requireRecipe(recipeId);
            if (!exist.getUserId().equals(userId)) {
                throw new BusinessException("无权操作他人的菜谱");
            }
            // 修改后重新审核：重置状态、清空拒绝理由（浏览量/点赞数保留）
            recipe.setId(recipeId);
            recipe.setStatus(0);
            recipe.setRejectReason(null);
            recipeMapper.updateContent(recipe);
            recipeMapper.deleteIngredients(recipeId);
            recipeMapper.deleteSteps(recipeId);
        }

        replaceChildren(recipe.getId(), req);
        return recipe.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long recipeId) {
        Recipe exist = requireRecipe(recipeId);
        if (!exist.getUserId().equals(userId)) {
            throw new BusinessException("无权操作他人的菜谱");
        }
        deleteWithChildren(recipeId);
    }

    // ==================== 用户端：公开浏览 ====================

    /** 公开分页列表：仅已通过 */
    public PageResult<Recipe> publicPage(int page, int size, Long categoryId, String keyword) {
        RecipeQuery query = new RecipeQuery();
        query.setStatus(1);
        query.setCategoryId(categoryId);
        query.setKeyword(StrUtil.blankToDefault(keyword, null));
        return doPage(page, size, query);
    }

    public List<Recipe> hot(int limit) {
        return recipeMapper.selectHot(limit);
    }

    /** 公开详情：仅已通过可看；访问即浏览量+1；携带登录态判断点赞/收藏状态 */
    public RecipeDetail detail(Long id, Long viewerId) {
        Recipe recipe = requireRecipe(id);
        if (recipe.getStatus() != 1) {
            throw new BusinessException("该菜谱不存在或尚未通过审核");
        }
        recipeMapper.updateViews(id);
        RecipeDetail detail = assembleDetail(recipe);
        detail.setLiked(interactionMapper.countLike(id, viewerId) > 0);
        detail.setFavorited(interactionMapper.countFavorite(id, viewerId) > 0);
        return detail;
    }

    /** 作者主页：其已通过菜谱分页 */
    public PageResult<Recipe> authorPublicPage(Long authorId, int page, int size) {
        RecipeQuery query = new RecipeQuery();
        query.setStatus(1);
        query.setUserId(authorId);
        return doPage(page, size, query);
    }

    // ==================== 管理端 ====================

    public PageResult<Recipe> adminPage(int page, int size, String keyword, Long categoryId, Integer status) {
        RecipeQuery query = new RecipeQuery();
        query.setKeyword(StrUtil.blankToDefault(keyword, null));
        query.setCategoryId(categoryId);
        query.setStatus(status);
        return doPage(page, size, query);
    }

    /** 管理端完整预览（不含浏览量自增，可看任何状态） */
    public RecipeDetail adminPreview(Long id) {
        return assembleDetail(requireRecipe(id));
    }

    /** 审核：通过(1) / 拒绝(2，理由必填)。仅待审核记录可审核。 */
    public void audit(Long id, AuditRequest req) {
        Recipe exist = requireRecipe(id);
        if (exist.getStatus() != 0) {
            throw new BusinessException("该菜谱不在待审核状态，请勿重复操作");
        }
        String reason = null;
        if (req.getStatus() == 2) {
            if (StrUtil.isBlank(req.getReason())) {
                throw new BusinessException("拒绝时必须填写拒绝理由");
            }
            reason = req.getReason().trim();
        }
        recipeMapper.updateAudit(id, req.getStatus(), reason);
    }

    public void adminDelete(Long id) {
        requireRecipe(id);
        deleteWithChildren(id);
    }

    // ==================== 私有方法 ====================

    private PageResult<Recipe> doPage(int page, int size, RecipeQuery query) {
        PageHelper.startPage(page, size);
        PageInfo<Recipe> pageInfo = new PageInfo<>(recipeMapper.selectByQuery(query));
        return PageResult.of(pageInfo);
    }

    private void replaceChildren(Long recipeId, RecipeSaveRequest req) {
        List<RecipeIngredient> ingredients = new ArrayList<>();
        int idx = 0;
        for (RecipeSaveRequest.IngredientItem item : req.getIngredients()) {
            RecipeIngredient ing = new RecipeIngredient();
            ing.setRecipeId(recipeId);
            ing.setName(item.getName().trim());
            ing.setAmount(item.getAmount().trim());
            ing.setSort(idx++);
            ingredients.add(ing);
        }
        recipeMapper.insertIngredients(ingredients);

        List<RecipeStep> steps = new ArrayList<>();
        idx = 0;
        for (RecipeSaveRequest.StepItem item : req.getSteps()) {
            RecipeStep step = new RecipeStep();
            step.setRecipeId(recipeId);
            step.setContent(item.getContent().trim());
            step.setSort(idx++);
            steps.add(step);
        }
        recipeMapper.insertSteps(steps);
    }

    private RecipeDetail assembleDetail(Recipe recipe) {
        RecipeDetail detail = new RecipeDetail();
        detail.setRecipe(recipe);
        detail.setIngredients(recipeMapper.selectIngredients(recipe.getId()));
        detail.setSteps(recipeMapper.selectSteps(recipe.getId()));
        return detail;
    }

    private void deleteWithChildren(Long recipeId) {
        recipeMapper.deleteIngredients(recipeId);
        recipeMapper.deleteSteps(recipeId);
        recipeMapper.deleteLikes(recipeId);
        recipeMapper.deleteFavorites(recipeId);
        recipeMapper.deleteComments(recipeId);
        recipeMapper.deleteById(recipeId);
    }

    private Recipe requireRecipe(Long id) {
        Recipe recipe = recipeMapper.selectDetail(id);
        if (recipe == null) {
            throw new BusinessException("菜谱不存在");
        }
        return recipe;
    }
}
