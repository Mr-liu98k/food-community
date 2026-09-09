package com.foodcommunity.mapper;

import com.foodcommunity.entity.Comment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论 Mapper（楼中楼）。SQL 见 CommentMapper.xml。
 */
public interface CommentMapper {

    int insert(Comment comment);

    Comment findById(@Param("id") Long id);

    /** 根评论分页（PageHelper 包裹），仅 parent_id=0 */
    List<Comment> selectRootPage(@Param("recipeId") Long recipeId);

    /** 一批根评论下的所有子评论（平铺，按时间正序） */
    List<Comment> selectChildrenByRoots(@Param("rootIds") List<Long> rootIds);

    /** 某父评论的直接子评论 ID（递归删除用） */
    List<Long> selectChildIds(@Param("parentId") Long parentId);

    int deleteByIds(@Param("ids") List<Long> ids);

    int countByRecipe(@Param("recipeId") Long recipeId);

    /** 我的评论分页（联菜谱标题） */
    List<Comment> selectMyPage(@Param("userId") Long userId);

    /** 该用户评论数（删除用户前引用检查） */
    long countByUser(@Param("userId") Long userId);

    /** 管理端评论分页：keyword 匹配内容或作者昵称 */
    List<Comment> selectAdminPage(@Param("keyword") String keyword);
}
