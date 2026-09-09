package com.foodcommunity.service;

import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.dto.BannerSaveRequest;
import com.foodcommunity.entity.Banner;
import com.foodcommunity.entity.Recipe;
import com.foodcommunity.mapper.BannerMapper;
import com.foodcommunity.mapper.RecipeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 轮播图模块。核心约束：banner 关联的菜谱必须【审核通过】，
 * 前台列表同样只放行 status=1 的菜谱（菜谱被拒/删除后自动不展示）。
 */
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerMapper bannerMapper;
    private final RecipeMapper recipeMapper;

    public List<Banner> frontList() {
        return bannerMapper.selectFrontList();
    }

    public PageResult<Banner> adminPage(int page, int size, String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Banner> pageInfo = new PageInfo<>(
                bannerMapper.selectAdminPage(keyword == null || keyword.isBlank() ? null : keyword.trim()));
        return PageResult.of(pageInfo);
    }

    public void create(BannerSaveRequest req) {
        validateRecipe(req.getRecipeId());
        bannerMapper.insert(toEntity(req));
    }

    public void update(Long id, BannerSaveRequest req) {
        requireBanner(id);
        validateRecipe(req.getRecipeId());
        Banner banner = toEntity(req);
        banner.setId(id);
        bannerMapper.update(banner);
    }

    public void delete(Long id) {
        requireBanner(id);
        bannerMapper.deleteById(id);
    }

    private Banner toEntity(BannerSaveRequest req) {
        Banner banner = new Banner();
        banner.setRecipeId(req.getRecipeId());
        banner.setImage(req.getImage() == null ? "" : req.getImage());
        banner.setSort(req.getSort() == null ? 0 : req.getSort());
        return banner;
    }

    /** 关联的菜谱必须存在且已通过审核 */
    private void validateRecipe(Long recipeId) {
        Recipe recipe = recipeMapper.selectDetail(recipeId);
        if (recipe == null || recipe.getStatus() != 1) {
            throw new BusinessException("只能关联【已审核通过】的菜谱");
        }
    }

    private Banner requireBanner(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException("轮播图不存在");
        }
        return banner;
    }
}
