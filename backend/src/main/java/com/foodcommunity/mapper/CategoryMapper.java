package com.foodcommunity.mapper;

import com.foodcommunity.entity.Category;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分类 Mapper。
 */
public interface CategoryMapper {

    /** 全量列表（按 name 模糊过滤，name 为空返回全部），可用于 PageHelper 分页 */
    List<Category> selectList(@Param("name") String name);

    Category selectById(@Param("id") Long id);

    Category selectByName(@Param("name") String name);

    int insert(Category category);

    int update(Category category);

    int deleteById(@Param("id") Long id);
}
