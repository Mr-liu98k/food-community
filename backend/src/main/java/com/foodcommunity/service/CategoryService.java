package com.foodcommunity.service;

import com.foodcommunity.common.BusinessException;
import com.foodcommunity.common.PageResult;
import com.foodcommunity.dto.CategorySaveRequest;
import com.foodcommunity.entity.Category;
import com.foodcommunity.mapper.CategoryMapper;
import com.foodcommunity.mapper.RecipeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类模块：前台全量列表 + 管理端增删改查。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final RecipeMapper recipeMapper;

    /** 前台分类列表（按 sort 排序，全量） */
    public List<Category> list() {
        return categoryMapper.selectList(null);
    }

    /** 管理端分页 */
    public PageResult<Category> page(int page, int size, String name) {
        PageHelper.startPage(page, size);
        PageInfo<Category> pageInfo = new PageInfo<>(categoryMapper.selectList(name));
        return PageResult.of(pageInfo);
    }

    public void create(CategorySaveRequest req) {
        checkNameUnique(req.getName(), null);
        Category category = new Category();
        category.setName(req.getName().trim());
        category.setSort(req.getSort() == null ? 0 : req.getSort());
        categoryMapper.insert(category);
    }

    public void update(Long id, CategorySaveRequest req) {
        Category exist = requireCategory(id);
        checkNameUnique(req.getName(), id);
        exist.setName(req.getName().trim());
        exist.setSort(req.getSort() == null ? 0 : req.getSort());
        categoryMapper.update(exist);
    }

    public void delete(Long id) {
        requireCategory(id);
        if (recipeMapper.countByCategory(id) > 0) {
            throw new BusinessException("该分类下存在菜谱，无法删除（可先迁移或删除菜谱）");
        }
        categoryMapper.deleteById(id);
    }

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        return category;
    }

    private void checkNameUnique(String name, Long excludeId) {
        Category exist = categoryMapper.selectByName(name.trim());
        if (exist != null && (excludeId == null || !exist.getId().equals(excludeId))) {
            throw new BusinessException("分类名称已存在");
        }
    }
}
